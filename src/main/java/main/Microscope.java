package main;

import static stage.IMotor.MIRROR;
import static stage.IMotor.Y_AXIS;
import static stage.IMotor.Z_AXIS;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.LutLoader;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.StackStatistics;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
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
import cam.NoopCamera;
import cam.SimulatedCamera;
import display.DisplayFrame;
import display.InfoFrame;
import display.PlaneDisplay;
import download.Download;
import download.DownloadManager;

/*
 * TODO move the sample back to the 'home position' after some idle time.
 *
 * TODO re-direct stdout to a shared dropbox file
 *
 */
public class Microscope implements AdminPanelListener {

	private static final boolean useScanMirror = true;

	private static final Logger logger;

	static {
		String date = new SimpleDateFormat("yyyMMdd").format(new Date());
		String dir = Preferences.getLogsDir();
		if(!dir.isEmpty()) {
			File logfile = new File(dir);
			if(!logfile.exists()) {
				if(!logfile.mkdirs()) {
					Mail.send("Error creating directory for log files",
							"Cannot create directory " + logfile.getAbsolutePath());
				}
			}
			logfile = new File(logfile, date + ".txt");
			System.out.println("logfile = " + logfile);
			System.setProperty(SimpleLogger.LOG_FILE_KEY, logfile.getAbsolutePath());
		}
		logger = LoggerFactory.getLogger(Microscope.class);
		System.out.println("logger = " + logger.getClass());
	}

	public static final int EXIT_NORMAL             =  0;
	public static final int EXIT_PREVIEW_ERROR      = -1;
	public static final int EXIT_STACK_ERROR        = -2;
	public static final int EXIT_MANUAL_LASER_ERROR = -3;
	public static final int EXIT_INITIALIZATION     = -4;
	public static final int EXIT_FATAL_ERROR        = -5;
	public static final int EXIT_BUTTON_ERROR       = -6;

	// TODO save these in Preferences
	private static final int STAGE_COM_PORT = 7;
	private static final int LASER_COM_PORT = 4;
	private static final int ARDUINO_COM_PORT = 3;

	private static enum Mode {
		NORMAL,
		ADMIN,
	}

	private final DecimalFormat df = new DecimalFormat("0.0000");

	private boolean simulated = false;

	private Mode mode = Mode.NORMAL;

	private boolean busy = false;
	private boolean timelapseRunning = false;

	private IMotor motor;
	private ILaser laser;
	private ICamera transmissionCamera, fluorescenceCamera;
	private AbstractButtons buttons;
	private KeyboardButtons keyboard;

	private final SingleElementThreadQueue mirrorQueue;

	private final PlaneDisplay displayPanel;
	private DisplayFrame displayWindow;
	private final AdminPanel adminPanel;
	private JConsole beanshell;
	private Interpreter beanshellInterpreter;
	private InfoFrame info;

	private final byte[] fluorescenceFrame, transmissionFrame;

	private static Microscope instance;

	private final ExecutorService exec = Executors.newSingleThreadExecutor();

	private Microscope(final boolean fatal) throws IOException, MotorException {

		logger.info("Initializing microscope");
		instance = this;

		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					displayWindow = new DisplayFrame(fatal);
					displayWindow.setVisible(true);
					displayWindow.setFullscreen(true);
				}
			});
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		if(fatal) {
			displayPanel = null;
			adminPanel = null;
			mirrorQueue = null;
			fluorescenceFrame = null;
			transmissionFrame = null;
			buttons = null;
			logger.info("Initialized fatal screen");
			return;
		}

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
				if(!timelapseRunning)
					shutdown(EXIT_NORMAL);
			}
		}, shutdownTime);

		Timer foregroundTimer = new Timer("eduSPIM foreground", true);
		foregroundTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if(displayWindow != null &&
						displayWindow.isFullscreen() &&
						mode == Mode.NORMAL &&
						info == null) {
					ToFront.toFront();
				}
			}
		}, 0, 5000);

		displayWindow.getMessages().println("Initializing hardware");

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
		displayWindow.setPlaneDisplay(displayPanel);

		final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		displayPanel.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if((e.getModifiers() & mask) != mask)
					return;
				if(busy)
					return;
				if(e.getKeyCode() == KeyEvent.VK_Q) {
					shutdown();
				} else if(e.getKeyCode() == KeyEvent.VK_F) {
					boolean fs = !displayWindow.isFullscreen();
					displayWindow.setFullscreen(fs);
					displayPanel.requestFocusInWindow();
					displayWindow.repaint();
				} else if(e.getKeyCode() == KeyEvent.VK_A) {
					if(mode == Mode.NORMAL) {
						mode = Mode.ADMIN;
						displayWindow.add(adminPanel, BorderLayout.WEST);
						displayWindow.validate();
						adminPanel.init();
						// displayPanel.requestFocusInWindow();
						startContinuousPreview();
					}
				} else if(e.getKeyCode() == KeyEvent.VK_B) {
					if(!beanshell.isShowing()) {
						displayWindow.add(beanshell, BorderLayout.NORTH);
						displayWindow.validate();
						beanshell.getViewport().getView().requestFocusInWindow();
					} else {
						displayWindow.remove(beanshell);
						displayWindow.validate();
						displayPanel.requestFocusInWindow();
					}
				} else if(e.getKeyCode() == KeyEvent.VK_V) {
					toggleSimulated();
				}
			}
		});
		final javax.swing.Timer cursorTimer = new javax.swing.Timer(2000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// displayPanel.setCursor(null);
				displayPanel.setCursor(
						displayPanel.getToolkit().createCustomCursor(new BufferedImage(
								3, 3, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "null"));
			}
		});
		displayPanel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				displayPanel.setCursor(Cursor.getDefaultCursor());
				cursorTimer.restart();
			}
		});

		final AWTButtons awtButtons = (buttons instanceof AWTButtons) ? (AWTButtons)buttons : null;
		final double yRelTmp = yRel;
		final int zTmp = z;

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				displayWindow.makeOverviewPanel(awtButtons);
				// displayWindow = new DisplayFrame(displayPanel, awtButtons, false);
				displayWindow.showSimulatedMessage(simulated);
		// 		displayWindow.pack();
				displayPanel.requestFocusInWindow();
				displayPanel.display(null, null, yRelTmp, zTmp);
				displayWindow.updateOverview(yRelTmp, (float)zTmp / ICamera.DEPTH);
			}
		});

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
		StringBuilder text = new StringBuilder();
		text.append("Hi,\n\n")
			.append("EduSPIM was just started successfully.\n\n");
		if(!Preferences.getLogsLink().isEmpty()) {
			text.append("Logs are here:\n");
			text.append(Preferences.getLogsLink()).append("\n\n");
		}
		if(!Preferences.getStacksLink().isEmpty()) {
			text.append("stack projections:\n");
			text.append(Preferences.getStacksLink()).append("\n\n");
		}
		if(!Preferences.getStatisticsLink().isEmpty()) {
			text.append("and statistics:\n");
			text.append(Preferences.getStatisticsLink()).append("\n\n");
		}
		text.append("Greetings,\nEduSPIM");
		Mail.send("Successful EduSPIM startup", Preferences.getMailto(), null, text.toString());
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
		displayWindow.getMessages().print("Initializing buttons via the Arduino...   ");
		try {
			buttons = new ArduinoButtons("COM" + ARDUINO_COM_PORT, this);
			displayWindow.getMessages().succeeded();
		} catch(Throwable e) {
			displayWindow.getMessages().failed();
			ExceptionHandler.handleException("Error initializing buttons", e);
			if(Preferences.getFailWithoutArduino()) {
				// We cannot do anything without buttons
				shutdown(EXIT_FATAL_ERROR);
			} else {
				displayWindow.getMessages().print("Initializign GUI buttons...   ");
				buttons = new AWTButtons();
				displayWindow.getMessages().succeeded();
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
			displayWindow.getMessages().print("Connecting motors...   ");
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

			motor.setTarget(Y_AXIS, Preferences.getStackYEnd());

			double mirrorPos = getMirrorPositionForZ(z0);
			if(useScanMirror)
				motor.setTarget(MIRROR, mirrorPos);

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
			displayWindow.getMessages().succeeded();
		} catch(Throwable e) {
			displayWindow.getMessages().failed();
			displayWindow.getMessages().print("Initializing the simulating motors instead...   ");
			ExceptionHandler.handleException("Error initializing the motors, using simulated motors instead", e);
			motor = new SimulatedMotor();
			try {
				motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
				motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
				motor.setTarget(Y_AXIS, Preferences.getStackYEnd());
				motor.setTarget(Z_AXIS, Preferences.getStackZStart());
				while(motor.isMoving(Z_AXIS) || motor.isMoving(Y_AXIS))
					sleep(50);
				displayWindow.getMessages().succeeded();
			} catch(Throwable ex) {
				displayWindow.getMessages().failed();
				ExceptionHandler.handleException("Error initializing simulated motors, exiting...", ex);
				shutdown(EXIT_FATAL_ERROR);
			}
			simulated = true;
		}
	}

	private void initCameras() {
		if(!simulated) {
			try {
				displayWindow.getMessages().print("Initializing cameras...   ");
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
				displayWindow.getMessages().succeeded();
				return;
			} catch(Throwable e) {
				ExceptionHandler.handleException("Error initializing the camera, using simulated camera instead instead", e);
			}
		}

		displayWindow.getMessages().print("Loading images for simulating camera...   ");
		simulated = true;
		String dir = System.getProperty("user.home") + File.separator + ".eduSPIM" + File.separator + "pre-acquired" + File.separator;
		String trpath = dir + "transmission.tif";
		ImagePlus trans = IJ.openImage(trpath);
		System.out.println("loaded " + trpath);
		String flpath = dir + "fluorescence.tif";
		ImagePlus fluor = IJ.openImage(flpath);
		System.out.println("loaded " + flpath);
		if(trans == null || fluor == null) {
			displayWindow.getMessages().failed();
			displayWindow.getMessages().print("Downloading pre-acquired data");
			sleep(1000);
			boolean approved = IJ.showMessageWithCancel(
					"Download example data",
					"It seems that the hardware is not fully functional. eduSPIM tries to start in \n" +
					"simulating mode, i.e. pretending full hardware functionality, but using pre-acquired \n" +
					"data. For this, example data is necessary. eduSPIM tried to load \n \n" +
					trpath + "\nand\n" + flpath +
					"\n \nbut it seems these files could not be opened correctly. Maybe you haven't installed\n" +
					"these example data on this computer? \n \n" +
					"Do you want to download and install it now? ");
			if(approved) {
				try {
					ImagePlus[] data = downloadExampleData(trans, fluor);
					trans = data[0];
					fluor = data[1];
					displayWindow.getMessages().succeeded();
				} catch(Exception e) {
					displayWindow.getMessages().failed();
					ExceptionHandler.handleException("Cannot download example data", e);
				}
			} else {
				displayWindow.getMessages().failed();
			}
		} else {
			displayWindow.getMessages().succeeded();
		}
		if(fluor == null || trans == null) {
			displayWindow.getMessages().print("Initializing fake cameras without image data...   ");
			fluorescenceCamera = new NoopCamera();
			transmissionCamera = new NoopCamera();
			displayWindow.getMessages().succeeded();
			return;
		}
		displayWindow.getMessages().print("Initializing simulating cameras with pre-acquired image data...   ");
		fluorescenceCamera = new SimulatedCamera(fluor);
		transmissionCamera = new SimulatedCamera(trans);
		displayWindow.getMessages().succeeded();
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
		displayWindow.showSimulatedMessage(simulated);
	}

	private void initLaser(double power) {
		displayWindow.getMessages().print("Initializing laser...   ");
		try {
			laser = new Toptica("COM" + LASER_COM_PORT, power);
			laser.setOff();
			displayWindow.getMessages().succeeded();
		} catch(Throwable e) {
			displayWindow.getMessages().failed();
			ExceptionHandler.handleException("Error initializing laser, using simulated laser instead", e);
			displayWindow.getMessages().print("Initializing simulating laser...   ");
			laser = new NoopLaser();
			displayWindow.getMessages().succeeded();
			simulated = true;
		}
	}

	private void loadBeanshellHistory() throws IOException, EvalError {
		File f = new File(System.getProperty("user.home"), ".eduSPIM_bsh_history");
		if(!f.exists())
			return;
		beanshellInterpreter.eval("microscope.beanshell.history.clear()");
		BufferedReader in = new BufferedReader(new FileReader(f));
		String line;
		while((line = in.readLine()) != null)
			beanshellInterpreter.eval("microscope.beanshell.history.add(\"" + line + "\")");
		in.close();
	}

	private void saveBeanshellHistory() {
		PrintStream out = null;
		try {
			Vector<?> history = (Vector<?>)beanshellInterpreter.get("microscope.beanshell.history");
			File f = new File(System.getProperty("user.home"), ".eduSPIM_bsh_history");
			out = new PrintStream(new FileOutputStream(f));
			for(Object o : history)
				out.println(o);
			out.close();
		} catch(Exception e) {
			ExceptionHandler.handleException("Error saving beanshell history", e);
		} finally {
			if(out != null)
				out.close();
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
		beanshellInterpreter = new Interpreter( beanshell );
		try {
			beanshellInterpreter.set("microscope", Microscope.this);
			beanshellInterpreter.eval("setAccessibility(true)");
			loadBeanshellHistory();
		} catch (Exception e) {
			ExceptionHandler.handleException("Error initializing beanshell", e);
		}
		new Thread( beanshellInterpreter ).start();
	}

	public void requestFocus() {
		displayPanel.requestFocusInWindow();
	}

	public synchronized boolean isBusy() {
		return busy;
	}

	public synchronized boolean isTimelapseRunning() {
		return timelapseRunning;
	}

	synchronized void resetBusy() {
		this.busy = false;
		displayWindow.clearBusy();
	}

	private void setBusy() {
		this.busy = true;
		displayWindow.showBusy("Busy...");
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

	synchronized void showInfo() {
		if(info == null) {
			if(mode == Mode.NORMAL) {
				logger.info("Show info screen");
				Statistics.incrementInfos();
			}
			info = new InfoFrame();
			info.addKeyListener(keyboard);
			setBusy();
		}
	}

	synchronized void closeInfo() {
		if(info != null) {
			if(mode == Mode.NORMAL)
				logger.info("Close info screen");
			info.dispose();
			info.removeKeyListener(keyboard);
			info = null;
			resetBusy();
		}
	}

	synchronized void startPreview(int button, int axis, double target) throws MotorException, CameraException, LaserException {
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
			displayWindow.updateOverview(yRel, (float)plane / ICamera.DEPTH);
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
		motor.setVelocity(MIRROR, IMotor.VEL_MAX_M);

		// log the move
		if(mode == Mode.NORMAL) {
			logger.info("Starting preview move: (" + df.format(yRelOrg) + ", " + planeOrg + ") -> (" + df.format(yRel) + ", " + plane + ")");
			Statistics.incrementMoves();
		}

		final BufferedImage im = displayPanel.getSnapshot();
		exec.submit(new Runnable() {
			@Override
			public void run() {
				// save the current snapshot
				try {
					String path = Preferences.getSnapshotPath();
					if(!path.isEmpty())
						ImageIO.write(im, "png", new File(path));
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
		timelapseRunning = true;
		long durationMillis = (long)(durationInHours * 60 * 60 * 1000);
		long intervalMillis = (long)(intervalInMinutes * 60 * 1000);
		int n = (int)Math.ceil(durationMillis / (double)intervalMillis);
		System.out.println("Running timelapse over " + n + " timepoints");
		long startMillis = System.currentTimeMillis();
		File dirf = new File(dir);
		long[] times = new long[n];
		double[] means = new double[n];
		File meansFile = new File(dirf, "means.csv");
		for(int it = 0; it < n; it++) {
			long time = 0;
			while((time = System.currentTimeMillis()) < startMillis + it * intervalMillis)
				sleep(100);

			String file = String.format("t%04d.png", it);
			String path = new File(dirf, file).getAbsolutePath();

			means[it] = acquireStack();
			times[it] = time;

			try {
				writeMeans(meansFile, times, means, it + 1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			saveSnapshot(path);
		}
		timelapseRunning = false;
	}

	void timelapse(
			double outerDurationInHours,
			double outerIntervalInMinutes,
			double innerDurationInHours,
			double innerIntervalInMinutes,
			String dir) throws MotorException, CameraException, LaserException {

		timelapseRunning = true;
		long innerDurationMillis = (long)(innerDurationInHours * 60 * 60 * 1000);
		long innerIntervalMillis = (long)(innerIntervalInMinutes * 60 * 1000);
		long outerDurationMillis = (long)(outerDurationInHours * 60 * 60 * 1000);
		long outerIntervalMillis = (long)(outerIntervalInMinutes * 60 * 1000);
		int nInner = (int)Math.ceil(innerDurationMillis / (double)innerIntervalMillis);
		int nOuter = (int)Math.ceil(outerDurationMillis / (double)outerIntervalMillis);
		long outerStartMillis = System.currentTimeMillis();
		File dirf = new File(dir);
		int N = nInner * nOuter;
		long[] times = new long[N];
		double[] means = new double[N];
		File meansFile = new File(dirf, "means.csv");
		for(int outerIt = 0, idx = 0; outerIt < nOuter; outerIt++) {
			while(System.currentTimeMillis() < outerStartMillis + outerIt * outerIntervalMillis)
				sleep(100);

			long innerStartMillis = System.currentTimeMillis();
			for(int innerIt = 0; innerIt < nInner; innerIt++, idx++) {
				long time = 0;
				while((time = System.currentTimeMillis()) < innerStartMillis + innerIt * innerIntervalMillis)
					sleep(100);

				String file = String.format("t%04d.png", idx);
				String path = new File(dirf, file).getAbsolutePath();
				means[idx] = acquireStack();
				times[idx] = time;

				try {
					writeMeans(meansFile, times, means, idx + 1);
				} catch (IOException e) {
					e.printStackTrace();
				}
				saveSnapshot(path);
			}
		}
		timelapseRunning = false;
	}

	void acquireStitchableData() throws MotorException, CameraException, LaserException {
		double minOverlap = 0.15;

		File dir = new File(System.getProperty("user.home") + "/pre-acquired/");
		String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
		dir = new File(dir, date);
		if(!dir.exists())
			dir.mkdirs();

		// setup motor positions
		double y0 = Preferences.getStackYStart();
		double y1 = Preferences.getStackYEnd();
		double dy = Preferences.getPixelWidth();

		double yRange = y1 - y0;
		double frameHeight = ICamera.HEIGHT * dy;
		int n = (int)Math.ceil(yRange / (frameHeight - minOverlap * frameHeight));
		double yDist = yRange / n;

		recordStack = true;
		for(int i = 0; i < n + 1; i++) {
			// save as hyperstacks
			motor.setTarget(Y_AXIS, y0 + i * yDist);
			while(motor.isMoving(Y_AXIS))
				; // wait
			acquireStack();
			ImagePlus transmission = WindowManager.getImage("transmission");
			ImagePlus fluorescence = WindowManager.getImage("fluorescence");

			int d = transmission.getStackSize();
			for(int z = 0; z < d; z++)
				fluorescence.getStack().addSlice("", transmission.getStack().getProcessor(z + 1));

			fluorescence.setOpenAsHyperStack(true);
			fluorescence.setDimensions(1, d, 2);
			transmission.close();
			String path = new File(dir, "tile" + (i + 1) + ".tif").getAbsolutePath();
			IJ.save(fluorescence, path);
			fluorescence.close();
		}
		ImagePlus stitched = Stitching.stitch(dir.getAbsolutePath(), 1, n + 1);
		Stitching.postProcess(stitched);
		stitched.show();
	}

	boolean recordStack = false;
	boolean animateStack = false;
	synchronized double acquireStack() throws MotorException, CameraException, LaserException {
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
			displayWindow.updateOverview(yRel, (float)plane / ICamera.DEPTH);
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
		displayWindow.updateOverview(yRel, 1);
		sleep(100); // delay to ensure rendering before changing stack mode
		displayPanel.setStackMode(true);

		ImageStack fluorescenceStack = null;
		ImageStack transmissionStack = null;
		if(recordStack || timelapseRunning) {
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
		ImageStack anim = null;
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
 			if(recordStack || timelapseRunning) {
				fluorescenceStack.addSlice("", fluorescenceFrame.clone());
				transmissionStack.addSlice("", transmissionFrame.clone());
			}
			displayPanel.display(fluorescenceFrame, null, yRel, i);
			displayWindow.updateOverview(yRel, (float)i / ICamera.DEPTH);
			if(animateStack) {
				sleep(100);
				ImageProcessor ip = new ColorProcessor(displayPanel.getSnapshot());
				if(anim == null)
					anim = new ImageStack(ip.getWidth(), ip.getHeight());
				anim.addSlice(ip);
			}
		}
		laser.setOff();
		fluorescenceCamera.stopSequence();
		transmissionCamera.stopSequence();
		if(animateStack) {
			if(IJ.getInstance() == null)
				new ij.ImageJ();
			new ImagePlus("animation", anim).show();
		}
		if(recordStack) {
			if(IJ.getInstance() == null)
				new ij.ImageJ();
			new ImagePlus("fluorescence", fluorescenceStack).show();
			new ImagePlus("transmission", transmissionStack).show();

		}
		double mean = 0;
		if(timelapseRunning) {
			mean = new StackStatistics(new ImagePlus("fluorescence", fluorescenceStack)).mean;
		}

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
		motor.setVelocity(MIRROR, IMotor.VEL_MAX_M);

		adminPanel.setPosition(motor.getPosition(Y_AXIS), motor.getPosition(Z_AXIS));

		// save the rendered projection // TODO only if we are in a head region
		final BufferedImage im = displayPanel.getSnapshot();
		exec.submit(new Runnable() {
			@Override
			public void run() {
				try {
					String dir = Preferences.getStacksDir();
					if(!dir.isEmpty()) {
						File f = new File(dir);
						if(!f.exists())
							f.mkdirs();
						String date = new SimpleDateFormat("yyyMMdd-HHmmss").format(new Date());
						ImageIO.write(im, "png", new File(f, date + ".png"));
					}
				} catch(Throwable e) {
					ExceptionHandler.handleException("Error saving projected stack", e);
				}

				// save the current snapshot
				try {
					String path = Preferences.getSnapshotPath();
					if(!path.isEmpty()) {
						BufferedImage im = displayPanel.getSnapshot();
						ImageIO.write(im, "png", new File(path));
					}
				} catch(Throwable e) {
					ExceptionHandler.handleException("Error saving snapshot", e);
				}
			}
		});
		synchronized(this) {
			resetBusy();
		}
		return mean;
	}

	public void writeMeans(File file, long[] times, double[] means, int nRows) throws IOException {
		PrintStream out = new PrintStream(new FileOutputStream(file));
		for(int r = 0; r < nRows && r < means.length; r++) {
			out.println(times[r] + "\t" + means[r]);
		}
		out.close();
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
		displayWindow.updateOverview(yRel, (float)z / ICamera.DEPTH);
	}

	private double zposBeforeManualLaserOn;
	public synchronized void manualLaserOn() throws LaserException, MotorException {
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

		laser.setPower(laser.getMaxPower());
		laser.setOn();
	}

	public synchronized void manualLaserOff() throws LaserException, MotorException {
		if(mode == Mode.NORMAL)
			logger.info("Manual laser off");

		laser.setTriggered();
		laser.setPower(Preferences.getLaserPower());
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
			StringBuilder text = new StringBuilder();
			text.append("Hi,\n\n")
				.append("EduSPIM was just shut down with exit status " + exitcode + ".\n\n");
			if(!Preferences.getLogsLink().isEmpty()) {
				text.append("Logs are here:\n");
				text.append(Preferences.getLogsLink()).append("\n\n");
			}
			if(!Preferences.getStacksLink().isEmpty()) {
				text.append("stack projections:\n");
				text.append(Preferences.getStacksLink()).append("\n\n");
			}
			if(!Preferences.getStatisticsLink().isEmpty()) {
				text.append("and statistics:\n");
				text.append(Preferences.getStatisticsLink()).append("\n\n");
			}
			text.append("Greetings,\nEduSPIM");
			Mail.send("EduSPIM shutdown (exit code " + exitcode + ")",
					Preferences.getMailto(),
					null,
					text.toString(),
					true);
			while(!mirrorQueue.isIdle())
				sleep(100);

			closeHardware();
			saveBeanshellHistory();

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
						displayWindow.updateOverview(yRel, (float)z / ICamera.DEPTH);
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
			if(!cancelled) {
				logger.info("Successfully changed EduSPIM settings.");
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
				try {
					String dir = Preferences.getPropertiesDir();
					if(!dir.isEmpty()) {
						File propdir = new File(dir);
						if(!propdir.exists()) {
							propdir.mkdirs();
						}
						String date = new SimpleDateFormat("yyyMMdd").format(new Date());
						String name = "EduSPIM." + date + ".props";
						Preferences.save(new File(propdir, name));
					}
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

	private ImagePlus[] downloadExampleData(final ImagePlus trans, final ImagePlus fluor) throws Exception {
		final File outdir = new File(System.getProperty("user.home") + File.separator
				+ ".eduSPIM" + File.separator + "pre-acquired");
		if (!outdir.exists())
			outdir.mkdirs();

		final ImagePlus[] ret = new ImagePlus[2];
		DownloadManager manager = new DownloadManager();
		Download tr = null, fl = null;
		if(trans == null) {
			tr = manager.addURL(
					"transmission.avi",
					"https://idisk-srv1.mpi-cbg.de/~bschmid/eduSPIM/transmission.avi",
					new File(outdir, "transmission.avi").getAbsolutePath(),
					new File(outdir, "transmission.tif").getAbsolutePath());
		}
		if(fluor == null) {
			fl = manager.addURL(
					"fluorescence.avi",
					"https://idisk-srv1.mpi-cbg.de/~bschmid/eduSPIM/fluorescence.avi",
					new File(outdir, "fluorescence.avi").getAbsolutePath(),
					new File(outdir, "fluorescence.tif").getAbsolutePath());
		}
		manager.setVisible(true);
		ret[0] = trans != null ? trans : tr.getImage();
		ret[1] = fluor != null ? fluor : fl.getImage();

		return ret;
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

//		SwingUtilities.invokeLater(new Runnable() {
//			@Override
//			public void run() {
				Microscope m = null;
				try {
					m = new Microscope(isFatal);
				} catch (Throwable e) {
					ExceptionHandler.handleException("Unexpected error during initialization", e);
					m.shutdown(EXIT_FATAL_ERROR);
				}
//			}
//		});
	}
}
