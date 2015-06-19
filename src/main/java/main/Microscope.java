package main;

import static stage.IMotor.MIRROR;
import static stage.IMotor.Y_AXIS;
import static stage.IMotor.Z_AXIS;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.LutLoader;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import laser.ILaser;
import laser.LaserException;
import laser.NoopLaser;
import laser.Toptica;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import stage.IMotor;
import stage.MotorException;
import stage.NativeMotor;
import stage.SimulatedMotor;
import windows.ToFront;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.util.JConsole;
import buttons.AWTButtons;
import buttons.AbstractButtons;
import buttons.ArduinoButtons;
import buttons.ButtonsException;
import buttons.KeyboardButtons;
import cam.CameraException;
import cam.ICamera;
import cam.NativeCamera;
import cam.SimulatedCamera;
import display.DisplayFrame;
import display.InfoFrame;
import display.PlaneDisplay;

/*
 * Things to test there:
 * - Sending mail
 * - Check preferences
 *
 * TODO move the sample back to the 'home position' after some idle time.
 *
 * TODO continuous preview mode in the admin panel for alignment
 *
 * TODO re-direct stdout to a shared dropbox file
 *
 */
public class Microscope implements AdminPanelListener {

	private static final boolean useScanMirror = false;

	private static final Logger logger;

	static {
		String date = new SimpleDateFormat("yyyMMdd").format(new Date());
		File logfile = new File(Preferences.getLogsDir());
		if(!logfile.exists()) {
			if(!logfile.mkdirs()) {
				Mail.send("Error creating directory for log files",
						"Cannot create directory " + logfile.getAbsolutePath());
			}
		}
		logfile = new File(logfile, date + ".txt");
		System.setProperty(SimpleLogger.LOG_FILE_KEY, logfile.getAbsolutePath());
		logger = LoggerFactory.getLogger(Microscope.class);
	}

	public static final int EXIT_NORMAL             =  0;
	public static final int EXIT_PREVIEW_ERROR      = -1;
	public static final int EXIT_STACK_ERROR        = -2;
	public static final int EXIT_MANUAL_LASER_ERROR = -3;
	public static final int EXIT_INITIALIZATION     = -4;
	public static final int EXIT_FATAL_ERROR        = -5;
	public static final int EXIT_BUTTON_ERROR       = -6;

	// TODO save these in Preferences
	private static final int STAGE_COM_PORT = 5;
	private static final int LASER_COM_PORT = 6;
	private static final int ARDUINO_COM_PORT = 3;

	private static enum Mode {
		NORMAL,
		ADMIN,
	}

	private final DecimalFormat df = new DecimalFormat("0.0000");

	private boolean simulated = false;

	private Mode mode = Mode.NORMAL;

	private boolean busy = false;

	private IMotor motor;
	private ILaser laser;
	private ICamera transmissionCamera, fluorescenceCamera;
	private AbstractButtons buttons;
	private KeyboardButtons keyboard;

	private final SingleElementThreadQueue mirrorQueue;

	private final PlaneDisplay displayPanel;
	private final DisplayFrame displayWindow;
	private final AdminPanel adminPanel;
	private JConsole beanshell;
	private InfoFrame info;

	private final byte[] fluorescenceFrame, transmissionFrame;

	private static Microscope instance;

	private final ExecutorService exec = Executors.newSingleThreadExecutor();

	private Microscope(boolean fatal) throws IOException, MotorException {

		logger.info("Initializing microscope");

		Timer shutdownTimer = new Timer("eduSPIM shutdown", true);
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 21);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		if(c.before(Calendar.getInstance()))
			c.add(Calendar.DAY_OF_MONTH, 1);
		Date shutdownTime = c.getTime();
		shutdownTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				shutdown(EXIT_NORMAL);
			}
		}, shutdownTime);

		Timer foregroundTimer = new Timer("eduSPIM foreground", true);
		foregroundTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					if(displayWindow != null &&
							displayWindow.isFullscreen() &&
							mode == Mode.NORMAL &&
							info == null) {
						ToFront.toFront();
					}
				} catch (AWTException e) {
					e.printStackTrace();
				}
			}
		}, 0, 5000);

		if(fatal) {
			displayPanel = null;
			displayWindow = new DisplayFrame(null, true);
			displayWindow.setVisible(true);
			displayWindow.setFullscreen(true);
			adminPanel = null;
			mirrorQueue = null;
			fluorescenceFrame = null;
			transmissionFrame = null;
			buttons = null;
			logger.info("Initialized fatal screen");
			return;
		}

		instance = this;

		initBeanshell();
		initHardware();

		double yRel = getCurrentRelativeYPos();
		int z = getCurrentPlane();

		fluorescenceFrame = new byte[ICamera.WIDTH * ICamera.HEIGHT];
		transmissionFrame = new byte[ICamera.WIDTH * ICamera.HEIGHT];

		URL url = getClass().getResource("/physics_inverted.lut");
		InputStream stream = url.openStream();
		IndexColorModel depthLut = LutLoader.open(stream);
		stream.close();

		mirrorQueue = new SingleElementThreadQueue();

		adminPanel = new AdminPanel(motor.getPosition(Y_AXIS), motor.getPosition(Z_AXIS));
		adminPanel.addAdminPanelListener(this);


		displayPanel = new PlaneDisplay(depthLut);
		displayPanel.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(busy)
					return;
				if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Q) {
					shutdown();
				} else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_F) {
					boolean fs = !displayWindow.isFullscreen();
					displayWindow.setFullscreen(fs);
					displayPanel.requestFocusInWindow();
					displayWindow.repaint();
				} else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A) {
					if(mode == Mode.NORMAL) {
						mode = Mode.ADMIN;
						displayWindow.add(adminPanel, BorderLayout.WEST);
						displayWindow.validate();
						adminPanel.init();
						// displayPanel.requestFocusInWindow();
						startContinuousPreview();
					}
				} else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_B) {
					if(!beanshell.isShowing()) {
						displayWindow.add(beanshell, BorderLayout.NORTH);
						displayWindow.validate();
						beanshell.getViewport().getView().requestFocusInWindow();
					} else {
						displayWindow.remove(beanshell);
						displayWindow.validate();
						displayPanel.requestFocusInWindow();
					}
				} else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {
					toggleSimulated();
				}
			}
		});
		displayWindow = new DisplayFrame(displayPanel, false);
		if(buttons instanceof AWTButtons)
			displayWindow.add(((AWTButtons)buttons).getPanel(), BorderLayout.EAST);
		displayWindow.pack();
		displayWindow.setVisible(true);
		displayWindow.setFullscreen(true);
		displayPanel.requestFocusInWindow();
		displayPanel.display(null, null, yRel, z);
		try {
			singlePreview(true, true);
		} catch(Exception e) {
			ExceptionHandler.handleException("Error getting initial preview", e);
		}

		SPIMButtonsListener list = new SPIMButtonsListener(this);
		buttons.addButtonsListener(list);
		keyboard.addButtonsListener(list);
		displayPanel.addKeyListener(keyboard);
		logger.info("Successfully initialized the microscope");
		Mail.send("Successful EduSPIM startup", Preferences.getMailto(), null,
				"Hi,\n\n"
				+ "EduSPIM was just started successfully.\n\n"
				+ "Logs are here:\n"
				+ Preferences.getLogsLink() + "\n\n"
				+ "stack projections:\n"
				+ Preferences.getStacksLink() + "\n\n"
				+ "and statistics:\n"
				+ Preferences.getStatisticsLink() + "\n\n"
				+ "Greetings,\nEduSPIM");
	}

	public static Microscope getInstance() {
		return instance;
	}

	public int getButtonDown() {
		int b = buttons.getButtonDown();
		if(b == -1)
			b = keyboard.getButtonDown();
		return b;
	}

	public void initHardware() {
		initMotor();
		initLaser(Preferences.getLaserPower());
		initButtons();

		// cameras go last: If anything went wrong, simulated is set to
		// true and we MUST use the simulated camera.
		initCameras();
	}

	public void closeHardware() {
		try {
			if(useScanMirror) {
				motor.setTarget(MIRROR, 1);
				while(motor.isMoving(MIRROR))
					;
			}
			motor.close();
		} catch(MotorException e) {
			ExceptionHandler.handleException("Error closing the motors", e);
		}
		try {
			fluorescenceCamera.close();
		} catch (CameraException e) {
			ExceptionHandler.handleException("Error closing the fluorescence camera", e);
		}
		try {
			transmissionCamera.close();
		} catch (CameraException e) {
			ExceptionHandler.handleException("Error closing the transmission camera", e);
		}
		try {
			laser.close();
		} catch(LaserException e) {
			ExceptionHandler.handleException("Error closing laser", e);
		}
		try {
			buttons.close();
		} catch(ButtonsException e) {
			ExceptionHandler.handleException("Error closing communication to the Arduino", e);
		}
	}

	private void initButtons() {
		try {
			buttons = new ArduinoButtons("COM" + ARDUINO_COM_PORT, this);
		} catch(Throwable e) {
			ExceptionHandler.handleException("Error initializing buttons", e);
			if(Preferences.getFailWithoutArduino()) {
				// We cannot do anything without buttons
				shutdown(EXIT_FATAL_ERROR);
			} else {
				buttons = new AWTButtons();
			}
		}
		keyboard = new KeyboardButtons();
	}

	private static boolean between(double v, double b1, double b2) {
		double i0 = Math.min(b1, b2);
		double i1 = Math.max(b1, b2);

		return v >= i0 && v <= i1;
	}

	private void initMotor() {
		try {
			motor = new NativeMotor(STAGE_COM_PORT);
			motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
			motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);

			double z0 = motor.getPosition(Z_AXIS);

			boolean inRange = between(
							z0,
							Preferences.getStackZStart(),
							Preferences.getStackZEnd());
			if(!inRange) {
				z0 = Preferences.getStackZStart();
				motor.setTarget(Z_AXIS, z0);
			}

			inRange = between(
					motor.getPosition(Y_AXIS),
					Preferences.getStackYStart(),
					Preferences.getStackYEnd());
			if(!inRange)
				motor.setTarget(Y_AXIS, Preferences.getStackYEnd());

			double mirrorPos = getMirrorPositionForZ(z0);
			if(useScanMirror)
				motor.setTarget(MIRROR, mirrorPos);
			else
				motor.setTarget(MIRROR, 2);

			// TODO only wait for the translation stages right now, later we'll have
			// to wait for the objective motor, too
			while(true) {
				boolean b = motor.isMoving(Y_AXIS) || motor.isMoving(Z_AXIS);
				if(!b && useScanMirror)
					b = b || motor.isMoving(MIRROR);
				if(!b)
					break;
				sleep(50);
			}
		} catch(Throwable e) {
			ExceptionHandler.handleException("Error initializing the motors, using simulated motors instead", e);
			motor = new SimulatedMotor();
			try {
				motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
				motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
				motor.setTarget(Y_AXIS, Preferences.getStackYStart());
				motor.setTarget(Z_AXIS, Preferences.getStackZStart());
				while(motor.isMoving(Z_AXIS) || motor.isMoving(Y_AXIS))
					sleep(50);
			} catch(Throwable ex) {
				ExceptionHandler.handleException("Error initializing simulated motors, exiting...", ex);
				shutdown(EXIT_FATAL_ERROR);
			}
			simulated = true;
		}
	}

	private void initCameras() {
		if(!simulated) {
			try {
				fluorescenceCamera = new NativeCamera(0,
						Preferences.getFCameraFramerate(),
						Preferences.getFCameraExposure(),
						Preferences.getFCameraGain());
				transmissionCamera = new NativeCamera(1,
						Preferences.getTCameraFramerate(),
						Preferences.getTCameraExposure(),
						Preferences.getTCameraGain());
				Preferences.setFCameraExposure(fluorescenceCamera.getExposuretime());
				Preferences.setFCameraFramerate(fluorescenceCamera.getFramerate());
				Preferences.setFCameraGain(fluorescenceCamera.getGain());
				Preferences.setTCameraExposure(transmissionCamera.getExposuretime());
				Preferences.setTCameraFramerate(transmissionCamera.getFramerate());
				Preferences.setTCameraGain(transmissionCamera.getGain());
				return;
			} catch(Throwable e) {
				ExceptionHandler.handleException("Error initializing the camera, using simulated camera instead instead", e);
			}
		}

		simulated = true;
		String dir = System.getProperty("user.home") + "/pre-acquired/";
		String path = dir + "transmission.tif";
		ImagePlus trans = IJ.openImage(path);
		System.out.println("loaded " + path);
		path = dir + "fluorescence.tif";
		ImagePlus fluor = IJ.openImage(path);
		System.out.println("loaded " + path);
		fluorescenceCamera = new SimulatedCamera(fluor);
		transmissionCamera = new SimulatedCamera(trans);
	}

	private void toggleSimulated() {
		try {
			transmissionCamera.close();
			fluorescenceCamera.close();
		} catch(Exception e) {
			ExceptionHandler.showException("Error toggling camera mode", e);
		}
		simulated = !simulated;
		initCameras();
	}

	private void initLaser(double power) {
		try {
			laser = new Toptica("COM" + LASER_COM_PORT, power);
			laser.setOff();
		} catch(Throwable e) {
			ExceptionHandler.handleException("Error initializing laser, using simulated laser instead", e);
			laser = new NoopLaser();
			simulated = true;
		}
	}

	public void initBeanshell() {
		beanshell = new JConsole();
		beanshell.getViewport().getView().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_B) {
					if(!beanshell.isShowing()) {
						displayWindow.add(beanshell, BorderLayout.NORTH);
						displayWindow.validate();
						beanshell.getViewport().getView().requestFocusInWindow();
					} else {
						displayWindow.remove(beanshell);
						displayWindow.validate();
						displayPanel.requestFocusInWindow();
					}
				}
			}
		});
		Interpreter interpreter = new Interpreter( beanshell );
		try {
			interpreter.set("microscope", Microscope.this);
		} catch (EvalError e) {
			ExceptionHandler.showException("Error evaluating beanshell commands", e);
		}
		new Thread( interpreter ).start();
	}

	public void requestFocus() {
		displayPanel.requestFocusInWindow();
	}

	public synchronized boolean isBusy() {
		return busy;
	}

	synchronized void resetBusy() {
		this.busy = false;
		displayWindow.clearMessage();
	}

	private void setBusy() {
		this.busy = true;
		displayWindow.showMessage("Busy...");
	}

	public IMotor getMotor() {
		return motor;
	}

	public ILaser getLaser() {
		return laser;
	}

	public ICamera getFluorescenceCamera() {
		return fluorescenceCamera;
	}

	public ICamera getTransmissionCamera() {
		return transmissionCamera;
	}

	int getCurrentPlane() throws MotorException {
		double zpos = motor.getPosition(Z_AXIS);
		return getPlaneForZ(zpos);
	}

	int getPlaneForZ(double zPos) {
		double z0 = Math.min(Preferences.getStackZStart(), Preferences.getStackZEnd());
		double z1 = Math.max(Preferences.getStackZStart(), Preferences.getStackZEnd());
		double zrel = 1 - (zPos - z0) / (z1 - z0); // let planes be inverted
		return (int)Math.round(zrel * ICamera.DEPTH);
	}

	double getCurrentRelativeYPos(double yPos) {
		double y0 = Math.min(Preferences.getStackYStart(), Preferences.getStackYEnd());
		double y1 = Math.max(Preferences.getStackYStart(), Preferences.getStackYEnd());
		return 1 - ((yPos - y0) / (y1 - y0));
	}

	double getCurrentRelativeYPos() throws MotorException {
		double ypos = motor.getPosition(Y_AXIS);
		return getCurrentRelativeYPos(ypos);
	}

	double getMirrorPositionForZ(double zPos) {
		return Preferences.getMirrorCoefficientM() * zPos + Preferences.getMirrorCoefficientT();
	}

	void showInfo() {
		if(info == null) {
			info = new InfoFrame();
			info.addKeyListener(keyboard);
			setBusy();
		}
	}

	void closeInfo() {
		if(info != null) {
			info.dispose();
			info.removeKeyListener(keyboard);
			info = null;
			resetBusy();
		}
	}

	void startPreview(int button, int axis, double target) throws MotorException, CameraException, LaserException {
		synchronized(this) {
			setBusy();
		}

		// get current plane
		double zPos = motor.getPosition(Z_AXIS);
		int plane = getPlaneForZ(zPos);
		double yPos = motor.getPosition(Y_AXIS);
		double yRel = getCurrentRelativeYPos(yPos);
		if(yRel < 0)
			yRel = 0;
		if(yRel > 1)
			yRel = 1;

		// remember old values
		double yRelOrg = yRel;
		double planeOrg = plane;

		// move mirror to start pos and wait until it's done
		if(axis == Z_AXIS) {
			double mirrorStart = getMirrorPositionForZ(zPos);
			if(useScanMirror) {
				motor.setTarget(MIRROR, mirrorStart);
				while(motor.isMoving(MIRROR))
					; // do nothing
			}
		}

		// set the speed of the motor according to the frame rate
		double framerate = fluorescenceCamera.getFramerate();
		double dz = (Preferences.getStackZEnd() - Preferences.getStackZStart()) / ICamera.DEPTH;

		motor.setVelocity(axis, Math.abs(dz * framerate));

		// set the speed of the mirror
		/*
		 * This is commented out since our current mirror doesn't allow to
		 * adjust its velocity, so the framerate needs to be adjusted to the
		 * mirror speed, and the latter is not touched at any time.
		 */
		/*
		if(axis == Z_AXIS) {
			double dMirror = Math.abs(
					getMirrorPositionForZ(Preferences.getStackZEnd()) -
					getMirrorPositionForZ(Preferences.getStackZStart())
				) / ICamera.DEPTH;
			motor.setVelocity(MIRROR, dMirror * framerate);
		}
		*/


		displayPanel.setStackMode(false);
		motor.setTarget(axis, target);
		// set the mirror target position
		if(axis == Z_AXIS) {
			if(useScanMirror) {
				double mirrorTgt = getMirrorPositionForZ(target);
				motor.setTarget(MIRROR, mirrorTgt);
			}
		}

		fluorescenceCamera.startSequence();
		transmissionCamera.startSequence();
		laser.setOn();
		do {
			if(axis == Y_AXIS) {
				yPos = target < yPos ? yPos - dz : yPos + dz; // positive ? yPos + dz : yPos - dz;
				yRel = getCurrentRelativeYPos(yPos);
				if(yRel < 0 || yRel > 1)
					break;
			} else if(axis == Z_AXIS) {
				zPos = target < zPos ? zPos - dz : zPos + dz;
				plane = getPlaneForZ(zPos);
				if(plane < 0 || plane >= ICamera.DEPTH)
					break;
			}

			if(fluorescenceCamera instanceof SimulatedCamera) {
				((SimulatedCamera) fluorescenceCamera).setYPosition(yRel);
				((SimulatedCamera) fluorescenceCamera).setZPosition(plane);
			}
			if(transmissionCamera instanceof SimulatedCamera) {
				((SimulatedCamera) transmissionCamera).setYPosition(yRel);
				((SimulatedCamera) transmissionCamera).setZPosition(plane);
			}
			fluorescenceCamera.getNextSequenceImage(fluorescenceFrame);
			transmissionCamera.getNextSequenceImage(transmissionFrame);
			displayPanel.display(fluorescenceFrame, transmissionFrame, yRel, plane);
		} while(getButtonDown() == button);

		laser.setOff();
		fluorescenceCamera.stopSequence();
		transmissionCamera.stopSequence();
		int mz = getCurrentPlane();
		System.out.println("plane = " + plane + " mz = " + mz + " ypos = " + yPos);

		// move to theoretic position?
		zPos = clamp(zPos, Preferences.getStackZStart(), Preferences.getStackZEnd());
		yPos = clamp(yPos, Preferences.getStackYStart(), Preferences.getStackYEnd());
		yRel = getCurrentRelativeYPos(yPos);
		plane = getPlaneForZ(zPos);
		double tgt = axis == Y_AXIS ? yPos : zPos;
		motor.setTarget(axis, tgt);
		if(axis == Z_AXIS) {
			if(useScanMirror)
				motor.setTarget(MIRROR, getMirrorPositionForZ(tgt));
		}

		while(motor.isMoving())
			sleep(50);

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
		// motor.setVelocity(MIRROR, IMotor.VEL_MAX_M);

		// log the move
		if(mode == Mode.NORMAL) {
			logger.info("Starting preview move: (" + df.format(yRelOrg) + ", " + planeOrg + ") -> (" + df.format(yRel) + ", " + plane + ")");
			Statistics.incrementMoves();
		}

		exec.submit(new Runnable() {
			@Override
			public void run() {
				// save the current snapshot
				try {
					BufferedImage im = displayPanel.getSnapshot();
					File f = new File(Preferences.getSnapshotPath());
					ImageIO.write(im, "png", f);
				} catch(Throwable e) {
					ExceptionHandler.handleException("Error saving snapshot", e);
				}
			}
		});

		adminPanel.setPosition(motor.getPosition(Y_AXIS), motor.getPosition(Z_AXIS));
		displayPanel.requestFocusInWindow();

		synchronized(this) {
			resetBusy();
		}
	}

	private double clamp(double v, double v0, double v1) {
		double b0 = Math.min(v0, v1);
		double b1 = Math.max(v0, v1);
		if(v < b0)
			return b0;
		if(v > b1)
			return b1;
		return v;
	}

	void saveSnapshot(String path) {
		try {
			BufferedImage im = displayPanel.getSnapshot();
			File f = new File(path);
			ImageIO.write(im, "png", f);
		} catch(Throwable e) {
			ExceptionHandler.handleException("Error saving snapshot", e);
		}
	}

	void timelapse(double durationInHours, double intervalInMinutes, String dir) throws MotorException, CameraException, LaserException {
		long durationMillis = (long)(durationInHours * 60 * 60 * 1000);
		long intervalMillis = (long)(intervalInMinutes * 60 * 1000);
		int n = (int)Math.ceil(durationMillis / (double)intervalMillis);
		System.out.println("Running timelapse over " + n + " timepoints");
		long startMillis = System.currentTimeMillis();
		File dirf = new File(dir);
		for(int it = 0; it < n; it++) {
			while(System.currentTimeMillis() < startMillis + it * intervalMillis)
				sleep(100);

			String file = String.format("t%04d.png", it);
			String path = new File(dirf, file).getAbsolutePath();
			acquireStack();
			saveSnapshot(path);
		}
	}

	boolean recordStack = false;
	void acquireStack() throws MotorException, CameraException, LaserException {
		synchronized(this) {
			setBusy();
		}

		double yRel = getCurrentRelativeYPos();

		if(mode == Mode.NORMAL) {
			logger.info("Acquiring stack: y = " + df.format(yRel));
			Statistics.incrementStacks();
		}

		// move motor and mirror back
		double zStart = Preferences.getStackZStart();
		motor.setTarget(Z_AXIS, zStart);
		if(useScanMirror)
			motor.setTarget(MIRROR, getMirrorPositionForZ(zStart));
		displayPanel.setStackMode(false);
		while(true) {
			boolean moving = motor.isMoving(Z_AXIS);
			if(!moving && useScanMirror)
				moving = moving || motor.isMoving(MIRROR);
			if(!moving)
				break;
			int plane = getCurrentPlane();
			displayPanel.display(null, null, yRel, plane);
		}

		// set the speed of the motor according to the frame rate
		double framerate = fluorescenceCamera.getFramerate();
		double dz = (Preferences.getStackZEnd() - Preferences.getStackZStart()) / ICamera.DEPTH;
		motor.setVelocity(Z_AXIS, Math.abs(dz * framerate));

		// set the speed of the mirror
		/*
		 * This is commented out since our current mirror doesn't allow to
		 * adjust its velocity, so the framerate needs to be adjusted to the
		 * mirror speed, and the latter is not touched at any time.
		 */
		/*
		double dMirror = Math.abs(
				getMirrorPositionForZ(Preferences.getStackZEnd()) -
				getMirrorPositionForZ(Preferences.getStackZStart())
			) / ICamera.DEPTH;
		motor.setVelocity(MIRROR, dMirror * framerate);
		*/

		displayPanel.display(null, null, yRel, ICamera.DEPTH - 1);
		sleep(100); // delay to ensure rendering before changing stack mode
		displayPanel.setStackMode(true);

		ImageStack fluorescenceStack = null;
		ImageStack transmissionStack = null;
		if(recordStack) {
			fluorescenceStack = new ImageStack(ICamera.WIDTH, ICamera.HEIGHT);
			transmissionStack = new ImageStack(ICamera.WIDTH, ICamera.HEIGHT);
		}

		double zEnd = Preferences.getStackZEnd();
		motor.setTarget(Z_AXIS, zEnd);
		if(useScanMirror)
			motor.setTarget(MIRROR, getMirrorPositionForZ(zEnd));

		fluorescenceCamera.startSequence();
		transmissionCamera.startSequence();
		laser.setOn();
		for(int i = ICamera.DEPTH - 1; i >= 0; i--) {
			if(fluorescenceCamera instanceof SimulatedCamera) {
				((SimulatedCamera) fluorescenceCamera).setYPosition(yRel);
				((SimulatedCamera) fluorescenceCamera).setZPosition(i);
			}
			fluorescenceCamera.getNextSequenceImage(fluorescenceFrame);
			if(transmissionCamera instanceof SimulatedCamera) {
				((SimulatedCamera) transmissionCamera).setYPosition(yRel);
				((SimulatedCamera) transmissionCamera).setZPosition(i);
			}
			transmissionCamera.getNextSequenceImage(transmissionFrame);
 			if(recordStack) {
				fluorescenceStack.addSlice("", fluorescenceFrame.clone());
				transmissionStack.addSlice("", transmissionFrame.clone());
			}
			displayPanel.display(fluorescenceFrame, null, yRel, i);
		}
		laser.setOff();
		fluorescenceCamera.stopSequence();
		transmissionCamera.stopSequence();
		if(recordStack) {
			new ij.ImageJ();
			new ImagePlus("fluorescence", fluorescenceStack).show();
			new ImagePlus("transmission", transmissionStack).show();
		}

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
		// motor.setVelocity(MIRROR, IMotor.VEL_MAX_M);

		adminPanel.setPosition(motor.getPosition(Y_AXIS), motor.getPosition(Z_AXIS));

		// save the rendered projection // TODO only if we are in a head region
		exec.submit(new Runnable() {
			@Override
			public void run() {
				try {
					BufferedImage im = displayPanel.getSnapshot();
					File f = new File(Preferences.getStacksDir());
					if(!f.exists())
						f.mkdirs();
					String date = new SimpleDateFormat("yyyMMdd").format(new Date());
					ImageIO.write(im, "png", new File(f, date + ".png"));
				} catch(Throwable e) {
					ExceptionHandler.handleException("Error saving projected stack", e);
				}

				// save the current snapshot
				try {
					BufferedImage im = displayPanel.getSnapshot();
					File f = new File(Preferences.getSnapshotPath());
					ImageIO.write(im, "png", f);
				} catch(Throwable e) {
					ExceptionHandler.handleException("Error saving snapshot", e);
				}
			}
		});
		synchronized(this) {
			resetBusy();
		}
	}

	void singlePreview(boolean trans, boolean fluor) throws CameraException, LaserException {
		if(!trans && !fluor)
			return;

		double yRel = displayPanel.getCurrentRelativeYPos();
		int z = displayPanel.getCurrentPlane();
		displayPanel.setStackMode(false);

		if(trans) {
			transmissionCamera.startSequence();
			if(transmissionCamera instanceof SimulatedCamera) {
				((SimulatedCamera) transmissionCamera).setYPosition(yRel);
				((SimulatedCamera) transmissionCamera).setZPosition(z);
			}
			transmissionCamera.getNextSequenceImage(transmissionFrame);
			transmissionCamera.stopSequence();
		}

		laser.setOn();
		if(fluor) {
			fluorescenceCamera.startSequence();
			if(fluorescenceCamera instanceof SimulatedCamera) {
				((SimulatedCamera) fluorescenceCamera).setYPosition(yRel);
				((SimulatedCamera) fluorescenceCamera).setZPosition(z);
			}
			fluorescenceCamera.getNextSequenceImage(fluorescenceFrame);
			fluorescenceCamera.stopSequence();
		}
		laser.setOff();

		// if both are acquired, display it normally
		if(trans && fluor)
			displayPanel.display(fluorescenceFrame, transmissionFrame, yRel, z);

		// if only one is present, display it as transmission image,
		// to avoid the translucent lookup table:
		else if(trans)
			displayPanel.display(null, transmissionFrame, yRel, z);
		else if(fluor)
			displayPanel.display(null, fluorescenceFrame, yRel, z);
	}

	private double zposBeforeManualLaserOn;
	public void manualLaserOn() throws LaserException, MotorException {
		if(mode == Mode.NORMAL) {
			logger.info("Manual laser on");
			Statistics.incrementLasers();
		}
		setBusy();
		// move stage away
		zposBeforeManualLaserOn = motor.getPosition(Z_AXIS);
		// check the faster way
		double d0 = Math.abs(zposBeforeManualLaserOn - Preferences.getStackZStart());
		double d1 = Math.abs(zposBeforeManualLaserOn - Preferences.getStackZEnd());
		double tgt = d0 < d1 ? Preferences.getStackZStart() : Preferences.getStackZEnd();
		motor.setTarget(Z_AXIS, tgt);
		while(motor.isMoving(Z_AXIS))
			; // wait

		laser.setOn();
	}

	public void manualLaserOff() throws LaserException, MotorException {
		if(mode == Mode.NORMAL)
			logger.info("Manual laser off");

		laser.setTriggered();
		// move mirror in place
		motor.setTarget(Z_AXIS, zposBeforeManualLaserOn);
		while(motor.isMoving(Z_AXIS))
			; // wait
		resetBusy();
	}

	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			ExceptionHandler.handleException("Interrupted during artificial delay", e);
		}
	}

	public void shutdown() {
		shutdown(0);
	}

	public void shutdown(int exitcode) {
		try {
			logger.info("Shutting down with exit code " + exitcode);
			Mail.send("EduSPIM shutdown (exit code " + exitcode + ")",
					Preferences.getMailto(),
					null,
					"Hi,\n\n"
					+ "EduSPIM was just shut down with exit status " + exitcode + ".\n\n"
					+ "Logs are here:\n"
					+ Preferences.getLogsLink() + "\n\n"
					+ "stack projections:\n"
					+ Preferences.getStacksLink() + "\n\n"
					+ "and statistics:\n"
					+ Preferences.getStatisticsLink() + "\n\n"
					+ "Greetings,\nEduSPIM",
					true);
			while(!mirrorQueue.isIdle())
				sleep(100);

			closeHardware();

			mirrorQueue.shutdown();
			displayWindow.dispose();
		} finally {
			System.exit(exitcode);
		}
	}

	public void setContinuousPreviewLaserOn(boolean b) {
		continuousPreviewManualLaserOn = b;
	}

	private boolean continuousPreviewRunning = false;
	private boolean continuousPreviewManualLaserOn = false;

	private void stopContinuousPreview() {
		continuousPreviewRunning = false;
	}

	private void startContinuousPreview() {
		continuousPreviewRunning = true;
		Thread previewThread = new Thread() {
			@Override
			public void run() {
				try {
					double yRel = getCurrentRelativeYPos();
					int z = getCurrentPlane();
					displayPanel.setStackMode(false);

					fluorescenceCamera.startPreview();
					if(fluorescenceCamera instanceof SimulatedCamera) {
						((SimulatedCamera) fluorescenceCamera).setYPosition(yRel);
						((SimulatedCamera) fluorescenceCamera).setZPosition(z);
					}
					transmissionCamera.startPreview();
					if(transmissionCamera instanceof SimulatedCamera) {
						((SimulatedCamera) transmissionCamera).setYPosition(yRel);
						((SimulatedCamera) transmissionCamera).setZPosition(z);
					}

					boolean updateFluorescence = false;
					boolean updateTransmission = true;
					do {
						z = getCurrentPlane();

						boolean fluorChanged = continuousPreviewManualLaserOn ||
								!mirrorQueue.isIdle() ||
								motor.isMoving();

						if(fluorChanged && !updateFluorescence) {
							updateFluorescence = true;
							laser.setOn();
						}
						else if(!fluorChanged && updateFluorescence) {
							updateFluorescence = false;
							laser.setOff();
						}

						if(updateFluorescence)
							fluorescenceCamera.getPreviewImage(fluorescenceFrame);
						if(updateTransmission)
							transmissionCamera.getPreviewImage(transmissionFrame);

						displayPanel.display(fluorescenceFrame, transmissionFrame, yRel, z);
					} while(continuousPreviewRunning);

					fluorescenceCamera.stopPreview();
					transmissionCamera.stopPreview();
					continuousPreviewManualLaserOn = false;
					laser.setOff();
					System.out.println("Stopped preview");
				} catch(Throwable e) {
					ExceptionHandler.showException("Error during preview", e);
					try {
							fluorescenceCamera.stopPreview();
					} catch(Throwable ex) {
						ExceptionHandler.showException("Error stopping preview", ex);
					}
					try {
							transmissionCamera.stopPreview();
					} catch(Throwable ex) {
						ExceptionHandler.showException("Error stopping preview", ex);
					}
				}
			}
		};
		previewThread.start();
	}

	/******************************************************
	 * AdminPanelListener interface
	 */
	@Override
	public void mirrorPositionChanged(final double z, final double m) {
		mirrorQueue.push(new Runnable() {
			@Override
			public void run() {
				try {
					motor.setTarget(MIRROR, m);
					motor.setTarget(Z_AXIS, z);
				} catch(Exception e) {
					ExceptionHandler.showException("Error setting mirror position", e);
				}
			} // run
		});
	}

	@Override
	public void motorPositionChanged(final double y, final double z) {
		mirrorQueue.push(new Runnable() {
			@Override
			public void run() {
				try {
					double m = getMirrorPositionForZ(z);
					if(useScanMirror)
						motor.setTarget(MIRROR, m);
					motor.setTarget(Z_AXIS, z);
					motor.setTarget(Y_AXIS, y);
				} catch(Exception e) {
					ExceptionHandler.showException("Error setting mirror position", e);
				}
			} // run
		});
	}

	@Override
	public void cameraParametersChanged() {
		mirrorQueue.push(new Runnable() {
			@Override
			public void run() {
				; // TODO should actually change the camera parameters here
			} // run
		});
	}

	@Override
	public void adminPanelDone(boolean cancelled) {
		if(mode == Mode.ADMIN) {
			stopContinuousPreview();
			boolean sampleExchanged = JOptionPane.showConfirmDialog(
					displayWindow,
					"Did you exchange the sample?\n\nThis information is needed for logs and statistics.",
					"Sample exchange",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null) == JOptionPane.YES_OPTION;
			if(sampleExchanged) {
				logger.info("Exchanged sample");
				Statistics.changeSample();
			}
			if(!cancelled) {
				logger.info("Successfully changed EduSPIM settings.");
				try {
					File propdir = new File(Preferences.getPropertiesDir());
					if(!propdir.exists()) {
						propdir.mkdirs();
					}
					String date = new SimpleDateFormat("yyyMMdd").format(new Date());
					String name = "EduSPIM." + date + ".props";
					Preferences.save(new File(propdir, name));
					// TODO mirror
					motor.setTarget(MIRROR, Preferences.getMirrorM1());
					while(motor.isMoving(MIRROR))
						;
					motor.setAbsolutePosition(MIRROR, 2);
				} catch(Exception e) {
					ExceptionHandler.showException("Error saving properties in the property history folder", e);
				}
			}
			mode = Mode.NORMAL;
			displayWindow.remove(adminPanel);
			displayWindow.validate();
			displayPanel.requestFocusInWindow();
			try {
				transmissionCamera.setFramerate(Preferences.getTCameraFramerate());
				transmissionCamera.setExposuretime(Preferences.getTCameraExposure());
				transmissionCamera.setGain(Preferences.getTCameraGain());
				fluorescenceCamera.setFramerate(Preferences.getFCameraFramerate());
				fluorescenceCamera.setExposuretime(Preferences.getFCameraExposure());
				fluorescenceCamera.setGain(Preferences.getFCameraGain());
				laser.setPower(Preferences.getLaserPower());
			} catch(Throwable t) {
				ExceptionHandler.showException("Cannot apply camera settings", t);
			}
		}
	}

	public static void main(String... args) {
		boolean fatal = false;
		for(String s : args) {
			if(s.trim().equalsIgnoreCase("--fatal"))
				fatal = true;
		}
		final boolean isFatal = fatal;

		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable e) {
			ExceptionHandler.handleException("Cannot set system L&F", e);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Microscope m = null;
				try {
					m = new Microscope(isFatal);
				} catch (Throwable e) {
					ExceptionHandler.handleException("Unexpected error during initialization", e);
					m.shutdown(EXIT_FATAL_ERROR);
				}
			}
		});
	}
}
