package main;

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
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import stage.IMotor;
import stage.MotorException;
import stage.NativeMotor;
import stage.SimulatedMotor;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.util.JConsole;
import buttons.AWTButtons;
import buttons.AbstractButtons;
import cam.CameraException;
import cam.ICamera;
import cam.NativeCamera;
import cam.SimulatedCamera;
import display.DisplayFrame;
import display.PlaneDisplay;

/*
 * Things to test there:
 * - Sending mail
 *
 * - Send email on startup/shutdown (maybe with log).
 *
 * - Check all caught exceptions, whether they should exit the program.
 *
 * - FATAL mode.
 *
 * - 6 buttons for y+, y-, z+, z-, laser, stack
 *   software simulation/ with arduino
 *
 * - 2nd camera for fluorescence, overlay for scroll mode,
 *   only fluorescence for stack mode
 *
 * - automatic image upload once per day
 *
 * - log button events to file
 *   (time stamp, button type)
 *
 */
public class Microscope implements AdminPanelListener {

	private static final Logger logger = LoggerFactory.getLogger(Microscope.class);

	public static final int EXIT_NORMAL         =  0;
	public static final int EXIT_PREVIEW_ERROR  = -1;
	public static final int EXIT_STACK_ERROR    = -2;
	public static final int EXIT_INITIALIZATION = -3;
	public static final int EXIT_FATAL_ERROR    = -4;

	private static final int COM_PORT = 7;
	private static final int BAUD_RATE = 38400;

	private static final String SNAPSHOT_FOLDER = System.getProperty("user.home") + File.separator + "EduSPIM_snapshots"; // TODO put on dropbox?

	private static enum Mode {
		NORMAL,
		ADMIN,
	}

	private boolean simulated = false;

	private Mode mode = Mode.NORMAL;

	private boolean busy = false;

	private IMotor motor;
	private ICamera transmissionCamera, fluorescenceCamera;
	private final AbstractButtons buttons;

	private final SingleElementThreadQueue mirrorQueue;

	private final PlaneDisplay displayPanel;
	private final DisplayFrame displayWindow;
	private final AdminPanel adminPanel;
	private JConsole beanshell;

	private final byte[] fluorescenceFrame, transmissionFrame;

	public Microscope(boolean fatal) throws IOException, MotorException {

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

		initBeanshell();
		initMotor(true);
		initCameras();

		double yRel = getCurrentRelativeYPos();

		fluorescenceFrame = new byte[ICamera.WIDTH * ICamera.HEIGHT];
		transmissionFrame = new byte[ICamera.WIDTH * ICamera.HEIGHT];

		URL url = getClass().getResource("/fire.lut");
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
		buttons = new AWTButtons();
		if(buttons instanceof AWTButtons)
			displayWindow.add(((AWTButtons)buttons).getPanel(), BorderLayout.EAST);
		displayWindow.pack();
		displayWindow.setVisible(true);
//		displayWindow.setFullscreen(true);
		displayPanel.requestFocusInWindow();
		displayPanel.display(null, null, yRel, 0);

		buttons.addButtonsListener(new SPIMButtonsListener(this));
		logger.info("Successfully initialized the microscope");
	}

	public void initMotor(boolean moveToStart) {
		try {
			motor = new NativeMotor(COM_PORT, BAUD_RATE); // TODO save parameters in Preferences
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

	public void initCameras() {
		ImagePlus trans = IJ.openImage(System.getProperty("user.home") + "/transmission.tif");
		if(!simulated) {
			try {
				fluorescenceCamera = new NativeCamera(0); // TODO check index
				transmissionCamera = new SimulatedCamera(trans); // TODO new NativeCamera(1);
				return;
			} catch(Throwable e) {
				ExceptionHandler.handleException("Error initializing the camera, using simulated camera instead instead", e);
			}
		}

		simulated = true;
		System.out.println("Loading the image");
		ImagePlus imp = IJ.openImage(System.getProperty("user.home") + "/HeadBack030_010um_3.tif");
		System.out.println("image loaded");
		fluorescenceCamera = new SimulatedCamera(imp);
		transmissionCamera = new SimulatedCamera(trans);
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

	public ICamera getFluorescenceCamera() {
		return fluorescenceCamera;
	}

	public ICamera getTransmissionCamera() {
		return transmissionCamera;
	}

	int getCurrentPlane() throws MotorException {
		double zpos = motor.getPosition(Z_AXIS);
		double zrel = (zpos - Preferences.getStackZStart()) / (Preferences.getStackZEnd() - Preferences.getStackZStart());
		return (int)Math.round(zrel * ICamera.DEPTH);
	}

	double getCurrentRelativeYPos() throws MotorException {
		double ypos = motor.getPosition(Y_AXIS);
		return (ypos - Preferences.getStackYStart()) / (Preferences.getStackYEnd() - Preferences.getStackYStart());
	}

	void startPreview(int button, int axis, boolean positive, double target) throws MotorException, CameraException {
		synchronized(this) {
			busy = true;
		}
		logger.info("Starting preview");
		System.out.println("startPreview: axis = " + axis + " target = " + target);
		// get current plane
		int plane = getCurrentPlane();
		double yPos = motor.getPosition(Y_AXIS);
		double yRel = (yPos - Preferences.getStackYStart()) / (Preferences.getStackYEnd() - Preferences.getStackYStart());
		System.out.println("start plane = " + plane);

		// set the speed of the motor according to the frame rate
		double framerate = fluorescenceCamera.getFramerate();
		double dz = (Preferences.getStackZEnd() - Preferences.getStackZStart()) / ICamera.DEPTH;
		motor.setVelocity(axis, dz * framerate);

		for(int i = 0; i < transmissionFrame.length; i++)
			transmissionFrame[i] = 100;

		displayPanel.setStackMode(false);
		motor.setTarget(axis, target);

		fluorescenceCamera.startSequence();
		transmissionCamera.startSequence();
		do {
			switch(axis) {
			case Y_AXIS:
				yPos = positive ? yPos + dz : yPos - dz;
				yRel = (yPos - Preferences.getStackYStart()) / (Preferences.getStackYEnd() - Preferences.getStackYStart());
				break;
			case Z_AXIS:
				plane = positive ? plane + 1 : plane - 1;
				break;
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

		fluorescenceCamera.stopSequence();
		transmissionCamera.stopSequence();
		int mz = getCurrentPlane();
		System.out.println("plane = " + plane + " mz = " + mz);

		// move to theoretic position?
		plane = Math.max(0, Math.min(plane, ICamera.DEPTH - 1));
		yPos = Math.max(Preferences.getStackYStart(), Math.min(yPos, Preferences.getStackYEnd()));
		double zPos = Preferences.getStackZStart() + plane * dz;
		double tgt = axis == Y_AXIS ? yPos : zPos;
		motor.setTarget(axis, tgt);
		motor.stop();
		while(motor.isMoving(axis))
			sleep(50);

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);

		adminPanel.setPosition(motor.getPosition(Y_AXIS), motor.getPosition(Z_AXIS));
		displayPanel.requestFocusInWindow();

		synchronized(this) {
			busy = false;
		}
	}

	void acquireStack() throws MotorException, CameraException {
		synchronized(this) {
			busy = true;
		}
		logger.info("Acquiring stack");

		double yRel = getCurrentRelativeYPos();

		// move the motor back
		motor.setTarget(Z_AXIS, Preferences.getStackZEnd());
		displayPanel.setStackMode(false);
		while(motor.isMoving()) {
			int plane = getCurrentPlane();
			displayPanel.display(null, null, yRel, plane);
		}

		// set the speed of the motor according to the frame rate
		double framerate = fluorescenceCamera.getFramerate();
		double dz = (Preferences.getStackZEnd() - Preferences.getStackZStart()) / ICamera.DEPTH;
		motor.setVelocity(Z_AXIS, dz * framerate);

		displayPanel.display(null, null, yRel, ICamera.DEPTH - 1);
		sleep(100); // delay to ensure rendering before changing stack mode
		displayPanel.setStackMode(true);
		motor.setTarget(Z_AXIS, Preferences.getStackZStart());

		fluorescenceCamera.startSequence();
		for(int i = ICamera.DEPTH - 1; i >= 0; i--) {
			if(fluorescenceCamera instanceof SimulatedCamera) {
				((SimulatedCamera) fluorescenceCamera).setYPosition(yRel);
				((SimulatedCamera) fluorescenceCamera).setZPosition(i);
			}
			fluorescenceCamera.getNextSequenceImage(fluorescenceFrame);
			displayPanel.display(fluorescenceFrame, null, yRel, i);
		}
		fluorescenceCamera.stopSequence();

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);

		adminPanel.setPosition(motor.getPosition(Y_AXIS), motor.getPosition(Z_AXIS));

		// save the rendered projection // TODO only if we are in a head region
		try {
			BufferedImage im = displayPanel.getSnapshot();
			File f = new File(SNAPSHOT_FOLDER);
			if(!f.exists())
				f.mkdirs();
			String date = new SimpleDateFormat("yyyMMdd").format(new Date());
			ImageIO.write(im, "png", new File(f, date + ".png"));
		} catch(Throwable e) {
			ExceptionHandler.handleException("Error saving rendered snapshot", e);
		}

		synchronized(this) {
			busy = false;
		}
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
		while(!mirrorQueue.isIdle())
			sleep(100);
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
		buttons.close();

		mirrorQueue.shutdown();
		displayWindow.dispose();
		System.exit(exitcode);
	}

	/******************************************************
	 * AdminPanelListener interaface
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
								} while(!mirrorQueue.isIdle()); // || mirror.isMoving()

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
				// TODO set mirror target pos to mirrorPos
			}
		});
	}

	@Override
	public void adminPanelDone() {
		if(mode == Mode.ADMIN) {
			mode = Mode.NORMAL;
			displayWindow.remove(adminPanel);
			displayWindow.validate();
			displayPanel.requestFocusInWindow();
		}
	}

	public static void main(String... args) {
		boolean fatal = false;
		for(String s : args) {
			if(s.trim().equalsIgnoreCase("--fatal"))
				fatal = true;
		}
		final boolean isFatal = fatal;
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
