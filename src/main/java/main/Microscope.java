package main;

import static stage.IMotor.MIRROR;
import static stage.IMotor.Y_AXIS;
import static stage.IMotor.Z_AXIS;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.LutLoader;

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
import java.util.Date;
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
import bsh.EvalError;
import bsh.Interpreter;
import bsh.util.JConsole;
import buttons.AWTButtons;
import buttons.AbstractButtons;
import buttons.ArduinoButtons;
import buttons.ButtonsException;
import cam.CameraException;
import cam.ICamera;
import cam.NativeCamera;
import cam.SimulatedCamera;
import display.DisplayFrame;
import display.PlaneDisplay;

/*
 * Things to test there:
 * - Sending mail
 * - Check preferences
 *
 * TODO info panel
 *
 * TODO automatically switch off and shutdown in the evening
 *
 * TODO move the sample back to the 'home position' after some idle time.
 *
 * TODO Autostart, auto close software, auto open software...
 *
 * TODO continuous preview mode in the admin panel for alignment
 *
 * TODO re-direct logs for today to a shared dropbox file
 */
public class Microscope implements AdminPanelListener {

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

	private final SingleElementThreadQueue mirrorQueue;

	private final PlaneDisplay displayPanel;
	private final DisplayFrame displayWindow;
	private final AdminPanel adminPanel;
	private JConsole beanshell;

	private final byte[] fluorescenceFrame, transmissionFrame;

	private static Microscope instance;

	// for saving snapshots TODO check whether that's necessary.
	private final ExecutorService exec = Executors.newSingleThreadExecutor();

	private Microscope(boolean fatal) throws IOException, MotorException {

		logger.info("Initializing microscope");

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
		initHardware(true);

		double yRel = getCurrentRelativeYPos();

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
					System.out.println("put window to fullscreen: " + fs);
					displayWindow.setFullscreen(fs);
					displayPanel.requestFocusInWindow();
					displayWindow.repaint();
				} else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A) {
					if(mode == Mode.NORMAL) {
						mode = Mode.ADMIN;
						adminPanel.init();
						displayWindow.add(adminPanel, BorderLayout.WEST);
						displayWindow.validate();
						displayPanel.requestFocusInWindow();
					}
				} else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_B) {
					if(!beanshell.isShowing()) {
						displayWindow.add(beanshell, BorderLayout.NORTH);
						displayWindow.validate();
					} else {
						displayWindow.remove(beanshell);
						displayWindow.validate();
						displayPanel.requestFocusInWindow();
					}
				}
			}
		});
		displayWindow = new DisplayFrame(displayPanel, false);
		if(buttons instanceof AWTButtons)
			displayWindow.add(((AWTButtons)buttons).getPanel(), BorderLayout.EAST);
		displayWindow.pack();
		displayWindow.setVisible(true);
		displayWindow.setFullscreen(true); // TODO fullscreen
		displayPanel.requestFocusInWindow();
		displayPanel.display(null, null, yRel, 0);

		buttons.addButtonsListener(new SPIMButtonsListener(this));
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

	public void initHardware(boolean moveMotorToStart) {
		initMotor(moveMotorToStart);
		initLaser(Preferences.getLaserPower());
		initButtons();

		// cameras go last: If anything went wrong, simulated is set to
		// true and we MUST use the simulated camera.
		initCameras();
	}

	public void closeHardware() {
		try {
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
	}

	private void initMotor(boolean moveToStart) {
		try {
			motor = new NativeMotor(STAGE_COM_PORT); // TODO save parameters in Preferences
			if(moveToStart) {
				motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
				motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
				motor.setTarget(Y_AXIS, Preferences.getStackYStart());
				motor.setTarget(Z_AXIS, Preferences.getStackZStart());
				while(motor.isMoving())
					sleep(50);
			}
		} catch(Throwable e) {
			ExceptionHandler.handleException("Error initializing the motors, using simulated motors instead", e);
			motor = new SimulatedMotor();
			if(moveToStart) {
				try {
					motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
					motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
					motor.setTarget(Y_AXIS, Preferences.getStackYStart());
					motor.setTarget(Z_AXIS, Preferences.getStackZStart());
					while(motor.isMoving())
						sleep(50);
				} catch(Throwable ex) {
					ExceptionHandler.handleException("Error initializing simulated motors, exiting...", ex);
					shutdown(EXIT_FATAL_ERROR);
				}
			}
			simulated = true;
		}
	}

	private void initCameras() {
		ImagePlus trans = IJ.openImage(System.getProperty("user.home") + "/transmission.tif");
//		if(!simulated) {
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
//		}

		simulated = true;
		System.out.println("Loading the image");
		ImagePlus imp = IJ.openImage(System.getProperty("user.home") + "/HeadBack030_010um_3.tif");
		System.out.println("image loaded");
		fluorescenceCamera = new SimulatedCamera(imp);
		transmissionCamera = new SimulatedCamera(trans);
	}

	private void initLaser(double power) {
		try {
			laser = new Toptica("COM" + LASER_COM_PORT, power); // TODO save parameters in Preferences
			laser.setOff();
		} catch(Throwable e) {
			ExceptionHandler.handleException("Error initializing laser, using simulated laser instead", e);
			laser = new NoopLaser();
			simulated = true;
		}
	}

	public void initBeanshell() {
		beanshell = new JConsole();
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
		double zrel = (zPos - Preferences.getStackZStart()) / (Preferences.getStackZEnd() - Preferences.getStackZStart());
		return (int)Math.round(zrel * ICamera.DEPTH);
	}

	double getCurrentRelativeYPos() throws MotorException {
		double ypos = motor.getPosition(Y_AXIS);
		return (ypos - Preferences.getStackYStart()) / (Preferences.getStackYEnd() - Preferences.getStackYStart());
	}

	double getMirrorPositionForZ(double zPos) {
		return Preferences.getMirrorCoefficientM() * zPos + Preferences.getMirrorCoefficientT();
	}

	void startPreview(int button, int axis, boolean positive, double target) throws MotorException, CameraException, LaserException {
		synchronized(this) {
			busy = true;
		}

		// get current plane
		double zPos = motor.getPosition(Z_AXIS);
		int plane = getPlaneForZ(zPos);
		double yPos = motor.getPosition(Y_AXIS);
		double yRel = (yPos - Preferences.getStackYStart()) / (Preferences.getStackYEnd() - Preferences.getStackYStart());
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
			motor.setTarget(MIRROR, mirrorStart);
			while(motor.isMoving(MIRROR))
				; // do nothing
		}

		// set the speed of the motor according to the frame rate
		double framerate = fluorescenceCamera.getFramerate();
		double dz = (Preferences.getStackZEnd() - Preferences.getStackZStart()) / ICamera.DEPTH;

		motor.setVelocity(axis, Math.abs(dz * framerate));

		// set the speed of the mirror
		if(axis == Z_AXIS) {
			double dMirror = Math.abs(
					getMirrorPositionForZ(Preferences.getStackZEnd()) -
					getMirrorPositionForZ(Preferences.getStackZStart())
				) / ICamera.DEPTH;
			motor.setVelocity(MIRROR, dMirror * framerate);
		}

		displayPanel.setStackMode(false);
		motor.setTarget(axis, target);
		// set the mirror target position
		if(axis == Z_AXIS) {
			double mirrorTgt = getMirrorPositionForZ(target);
			motor.setTarget(MIRROR, mirrorTgt);
		}

		fluorescenceCamera.startSequence();
		transmissionCamera.startSequence();
		laser.setOn();
		do {
			if(axis == Y_AXIS) {
				yPos = positive ? yPos + dz : yPos - dz;
				yRel = (yPos - Preferences.getStackYStart()) / (Preferences.getStackYEnd() - Preferences.getStackYStart());
			} else if(axis == Z_AXIS) {
				plane = positive ? plane + 1 : plane - 1;
			}

			// stop if moving out of area
			if(plane < 0 || plane >= ICamera.DEPTH ||
					yRel < 0 || yRel > 1)
				break;

			if(fluorescenceCamera instanceof SimulatedCamera) {
				((SimulatedCamera) fluorescenceCamera).setYPosition(yRel);
				((SimulatedCamera) fluorescenceCamera).setZPosition(plane);
			}
			if(transmissionCamera instanceof SimulatedCamera) {
				((SimulatedCamera) transmissionCamera).setYPosition(yRel);
				((SimulatedCamera) transmissionCamera).setZPosition(0);
			}
			fluorescenceCamera.getNextSequenceImage(fluorescenceFrame);
			transmissionCamera.getNextSequenceImage(transmissionFrame);
			displayPanel.display(fluorescenceFrame, transmissionFrame, yRel, plane);
		} while(buttons.getButtonDown() == button);

		laser.setOff();
		fluorescenceCamera.stopSequence();
		transmissionCamera.stopSequence();
		int mz = getCurrentPlane();
		System.out.println("plane = " + plane + " mz = " + mz + " ypos = " + yPos);

		// move to theoretic position?
		plane = Math.max(0, Math.min(plane, ICamera.DEPTH - 1));
		yPos = Math.max(Preferences.getStackYStart(), Math.min(yPos, Preferences.getStackYEnd()));
		yRel = (yPos - Preferences.getStackYStart()) / (Preferences.getStackYEnd() - Preferences.getStackYStart());
		zPos = Preferences.getStackZStart() + plane * dz;
		double tgt = axis == Y_AXIS ? yPos : zPos;
		motor.setTarget(axis, tgt);
		if(axis == Z_AXIS)
			motor.setTarget(MIRROR, getMirrorPositionForZ(tgt));

		while(motor.isMoving())
			sleep(50);

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
		motor.setVelocity(MIRROR, IMotor.VEL_MAX_M);

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
			busy = false;
		}
	}

	void acquireStack() throws MotorException, CameraException, LaserException {
		synchronized(this) {
			busy = true;
		}

		double yRel = getCurrentRelativeYPos();

		if(mode == Mode.NORMAL) {
			logger.info("Acquiring stack: y = " + df.format(yRel));
			Statistics.incrementStacks();
		}

		// move motor and mirror back
		double zStart = Preferences.getStackZEnd();
		motor.setTarget(Z_AXIS, zStart);
		motor.setTarget(MIRROR, getMirrorPositionForZ(zStart));
		displayPanel.setStackMode(false);
		while(motor.isMoving(Z_AXIS) || motor.isMoving(MIRROR)) {
			int plane = getCurrentPlane();
			displayPanel.display(null, null, yRel, plane);
		}

		// set the speed of the motor according to the frame rate
		double framerate = fluorescenceCamera.getFramerate();
		double dz = (Preferences.getStackZEnd() - Preferences.getStackZStart()) / ICamera.DEPTH;
		motor.setVelocity(Z_AXIS, Math.abs(dz * framerate));

		// set the speed of the mirror
		double dMirror = Math.abs(
				getMirrorPositionForZ(Preferences.getStackZEnd()) -
				getMirrorPositionForZ(Preferences.getStackZStart())
			) / ICamera.DEPTH;
		motor.setVelocity(MIRROR, dMirror * framerate);

		displayPanel.display(null, null, yRel, ICamera.DEPTH - 1);
		sleep(100); // delay to ensure rendering before changing stack mode
		displayPanel.setStackMode(true);
		double zEnd = Preferences.getStackZStart();
		motor.setTarget(Z_AXIS, zEnd);
		motor.setTarget(MIRROR, getMirrorPositionForZ(zEnd));

		fluorescenceCamera.startSequence();
		laser.setOn();
		for(int i = ICamera.DEPTH - 1; i >= 0; i--) {
			if(fluorescenceCamera instanceof SimulatedCamera) {
				((SimulatedCamera) fluorescenceCamera).setYPosition(yRel);
				((SimulatedCamera) fluorescenceCamera).setZPosition(i);
			}
			fluorescenceCamera.getNextSequenceImage(fluorescenceFrame);
			displayPanel.display(fluorescenceFrame, null, yRel, i);
		}
		laser.setOff();
		fluorescenceCamera.stopSequence();

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
		motor.setVelocity(MIRROR, IMotor.VEL_MAX_M);

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
			busy = false;
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
				((SimulatedCamera) transmissionCamera).setZPosition(0);
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

	public void manualLaserOn() throws LaserException, MotorException {
		if(mode == Mode.NORMAL) {
			logger.info("Manual laser on");
			Statistics.incrementLasers();
		}
		// move mirror away
		double mirror = getMirrorPositionForZ(Preferences.getStackZStart());
		motor.setTarget(MIRROR, mirror);
		while(motor.isMoving(MIRROR))
			; // wait

		laser.setOn();
	}

	public void manualLaserOff() throws LaserException, MotorException {
		if(mode == Mode.NORMAL)
			logger.info("Manual laser off");

		laser.setTriggered();
		// move mirror in place
		double mirror = getMirrorPositionForZ(motor.getPosition(Z_AXIS));
		motor.setTarget(MIRROR, mirror);
		while(motor.isMoving(MIRROR))
			; // wait
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
		System.exit(exitcode);
	}

	/******************************************************
	 * AdminPanelListener interface
	 */
	@Override
	public void mirrorPositionChanged(final double pos) {
		mirrorQueue.push(new Runnable() {
			@Override
			public void run() {
				if(!fluorescenceCamera.isPreviewRunning()) {
					System.out.println("Starting preview");
					Thread previewThread = new Thread() {
						@Override
						public void run() {
							try {
								displayPanel.setStackMode(false);
								fluorescenceCamera.startPreview();
								double yRel = getCurrentRelativeYPos();
								int z = getCurrentPlane();
								if(fluorescenceCamera instanceof SimulatedCamera) {
									((SimulatedCamera) fluorescenceCamera).setYPosition(yRel);
									((SimulatedCamera) fluorescenceCamera).setZPosition(z);
								}
								do {
									fluorescenceCamera.getPreviewImage(fluorescenceFrame);
									// display it as transmission image to avoid the translucent
									// lookup table:
									displayPanel.display(null, fluorescenceFrame, yRel, z);
								} while(!mirrorQueue.isIdle() || motor.isMoving(MIRROR));

								fluorescenceCamera.stopPreview();
								System.out.println("Stopped preview");
							} catch(Throwable e) {
								ExceptionHandler.showException("Error during preview", e);
								try {
									fluorescenceCamera.stopPreview();
								} catch(Throwable ex) {
									ExceptionHandler.showException("Error stopping preview", ex);
								}
							}
						}
					};
					previewThread.start();
				}
				try {
					motor.setTarget(MIRROR, pos);
				} catch(Exception e) {
					ExceptionHandler.showException("Error setting mirror position", e);
				}
			} // run
		});
	}

	@Override
	public void adminPanelDone(boolean cancelled) {
		if(mode == Mode.ADMIN) {
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
