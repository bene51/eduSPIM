package main;

import static stage.IMotor.Y_AXIS;
import static stage.IMotor.Z_AXIS;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.LutLoader;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.SwingUtilities;

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
 * - 6 buttons for y+, y-, z+, z-, laser, stack
 *   software simulation/ with arduino
 *
 * - 2nd camera for fluorescence, overlay for scroll mode,
 *   only fluorescence for stack mode
 *
 * - automatic image upload once per day
 *
 * - panel for 'motor moving', etc.
 *
 * - mode 'change sample'
 *     - new positions for y/z start and end
 *     - new reference image/histogram
 *
 * - log button events to file
 *   (time stamp, button type)
 *
 */
public class Microscope implements AdminPanelListener {

	public static final int EXIT_NORMAL         =  0;
	public static final int EXIT_PREVIEW_ERROR  = -1;
	public static final int EXIT_STACK_ERROR    = -2;
	public static final int EXIT_INITIALIZATION = -3;
	public static final int EXIT_FATAL_ERROR    = -4;

	private static final int COM_PORT = 7;
	private static final int BAUD_RATE = 38400;

	private static enum Mode {
		NORMAL,
		ADMIN,
	}

	private boolean simulated = false;

	private Mode mode = Mode.NORMAL;

	private boolean busy = false;

	private IMotor motor;
	private ICamera camera;
	private final AbstractButtons buttons;

	private final SingleElementThreadQueue mirrorQueue;

	private final PlaneDisplay displayPanel;
	private final DisplayFrame displayWindow;
	private final AdminPanel adminPanel;
	private JConsole beanshell;

	private final byte[] fluorescenceFrame, transmissionFrame;

	public Microscope() throws IOException, MotorException {

		initBeanshell();
		initMotor(true);
		initCamera();

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
		displayWindow = new DisplayFrame(displayPanel);
		buttons = new AWTButtons();
		if(buttons instanceof AWTButtons)
			displayWindow.add(((AWTButtons)buttons).getPanel(), BorderLayout.EAST);
		displayWindow.pack();
		displayWindow.setVisible(true);
//		displayWindow.setFullscreen(true);
		displayPanel.requestFocusInWindow();
		displayPanel.display(null, null, yRel, 0);

		buttons.addButtonsListener(new SPIMButtonsListener(this));
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
			ExceptionHandler.handleException(e);
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
					ExceptionHandler.handleException(ex);
					shutdown(EXIT_FATAL_ERROR);
				}
			}
			simulated = true;
		}
	}

	public void initCamera() {
		if(!simulated) {
			try {
				camera = new NativeCamera(0);
				return;
			} catch(Throwable e) {
				ExceptionHandler.handleException(e);
			}
		}

		simulated = true;
		System.out.println("Loading the image");
		ImagePlus imp = IJ.openImage(System.getProperty("user.home") + "/HeadBack030_010um_3.tif");
		System.out.println("image loaded");
		camera = new SimulatedCamera(imp);
	}

	public void initBeanshell() {
		beanshell = new JConsole();
		Interpreter interpreter = new Interpreter( beanshell );
		try {
			interpreter.set("microscope", Microscope.this);
		} catch (EvalError e) {
			ExceptionHandler.handleException(e);
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
		System.out.println("startPreview: axis = " + axis + " target = " + target);
		// get current plane
		int plane = getCurrentPlane();
		double yPos = motor.getPosition(Y_AXIS);
		double yRel = (yPos - Preferences.getStackYStart()) / (Preferences.getStackYEnd() - Preferences.getStackYStart());
		System.out.println("start plane = " + plane);

		// set the speed of the motor according to the frame rate
		double framerate = camera.getFramerate();
		double dz = (Preferences.getStackZEnd() - Preferences.getStackZStart()) / ICamera.DEPTH;
		motor.setVelocity(axis, dz * framerate);

		for(int i = 0; i < transmissionFrame.length; i++)
			transmissionFrame[i] = 100;

		displayPanel.setStackMode(false);
		motor.setTarget(axis, target);

		camera.startSequence();
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

			if(camera instanceof SimulatedCamera) {
				((SimulatedCamera) camera).setYPosition(yRel);
				((SimulatedCamera) camera).setZPosition(plane);
			}
			camera.getNextSequenceImage(fluorescenceFrame);
			displayPanel.display(fluorescenceFrame, transmissionFrame, yRel, plane);
		} while(buttons.getButtonDown() == button);

		camera.stopSequence();
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

		double yRel = getCurrentRelativeYPos();

		// move the motor back
		motor.setTarget(Z_AXIS, Preferences.getStackZEnd());
		displayPanel.setStackMode(false);
		while(motor.isMoving()) {
			int plane = getCurrentPlane();
			displayPanel.display(null, null, yRel, plane);
		}

		// set the speed of the motor according to the frame rate
		double framerate = camera.getFramerate();
		double dz = (Preferences.getStackZEnd() - Preferences.getStackZStart()) / ICamera.DEPTH;
		motor.setVelocity(Z_AXIS, dz * framerate);

		displayPanel.display(null, null, yRel, ICamera.DEPTH - 1);
		sleep(100); // delay to ensure rendering before changing stack mode
		displayPanel.setStackMode(true);
		motor.setTarget(Z_AXIS, Preferences.getStackZStart());

		camera.startSequence();
		for(int i = ICamera.DEPTH - 1; i >= 0; i--) {
			if(camera instanceof SimulatedCamera) {
				((SimulatedCamera) camera).setYPosition(yRel);
				((SimulatedCamera) camera).setZPosition(i);
			}
			camera.getNextSequenceImage(fluorescenceFrame);
			displayPanel.display(fluorescenceFrame, null, yRel, i);
		}
		camera.stopSequence();

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);

		adminPanel.setPosition(motor.getPosition(Y_AXIS), motor.getPosition(Z_AXIS));

		synchronized(this) {
			busy = false;
		}
	}

	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			ExceptionHandler.handleException(e);
		}
	}

	public void shutdown() {
		shutdown(0);
	}

	public void shutdown(int exitcode) {
		// TODO log it somewhere
		while(!mirrorQueue.isIdle())
			sleep(100);
		try {
			motor.close();
		} catch(MotorException e) {
			ExceptionHandler.handleException(e);
		}
		try {
			camera.close();
		} catch (CameraException e) {
			ExceptionHandler.handleException(e);
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
				if(!camera.isPreviewRunning()) {
					System.out.println("Starting preview");
					Thread previewThread = new Thread() {
						@Override
						public void run() {
							try {
								displayPanel.setStackMode(false);
								camera.startPreview();
								double yRel = getCurrentRelativeYPos();
								int z = getCurrentPlane();
								if(camera instanceof SimulatedCamera) {
									((SimulatedCamera) camera).setYPosition(yRel);
									((SimulatedCamera) camera).setZPosition(z);
								}
								while(!mirrorQueue.isIdle()) { // || mirror.isMoving()
									camera.getPreviewImage(fluorescenceFrame);
									displayPanel.display(fluorescenceFrame, null, yRel, z);
								}
								camera.stopPreview();
								System.out.println("Stopped preview");
							} catch(Throwable e) {
								ExceptionHandler.showException(e);
								try {
									camera.stopPreview();
								} catch(Throwable ex) {
									ExceptionHandler.showException(ex);
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
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					new Microscope();
				} catch (Throwable e) {
					ExceptionHandler.handleException(e);
					System.exit(EXIT_FATAL_ERROR);
				}
			}
		});
	}
}
