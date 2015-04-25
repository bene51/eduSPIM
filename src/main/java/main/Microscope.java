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
import stage.SimulatedMotor;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.util.JConsole;
import buttons.AWTButtons;
import buttons.AbstractButtons;
import buttons.ButtonsListener;
import cam.ICamera;
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
 * - y scrolling
 *
 * - mail on exception
 *
 * - automatic image upload once per day
 *
 * - panel for 'motor moving', etc.
 *
 * - mode 'change sample'
 *     - new positions for y/z start and end
 *     - new reference image/histogram
 *
 * - mode 'calibrate mirror'
 *
 * - log button events to file
 *   (time stamp, button type)
 *
 */
public class Microscope {

	private static final int COM_PORT = 7;
	private static final int BAUD_RATE = 38400;

	private static enum Mode {
		NORMAL,
		ADMIN,
	}

	private Mode mode = Mode.NORMAL;

	private boolean acquiringStack = false;

	private final IMotor motor;
	private final ICamera camera;
	private final AbstractButtons buttons;

	private final SingleElementThreadQueue mirrorQueue;

	private final PlaneDisplay displayPanel;
	private final DisplayFrame displayWindow;
	private final AdminPanel adminPanel;
	private final JConsole beanshell;

	// TODO whenever there occurs an exception with the camera, switch to artificial camera.
	// TODO whenever there occurs an exception with the stage, switch to artificial camera and stage.
	// TODO same for mirror once it's implemented
	// TODO acquireStack should not move the motor in y direction
	public Microscope() throws IOException { // TODO catch exception

		beanshell = new JConsole();
		Interpreter interpreter = new Interpreter( beanshell );
		try {
			interpreter.set("microscope", Microscope.this);
		} catch (EvalError e) {
			e.printStackTrace();
		}
		new Thread( interpreter ).start();



		motor = new SimulatedMotor();
		// final IMotor motor = new NativeMotor(COM_PORT, BAUD_RATE);
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
		motor.setTarget(Y_AXIS, Preferences.getStackYStart());
		motor.setTarget(Z_AXIS, Preferences.getStackZStart());

		while(motor.isMoving())
			sleep(100);

		double yRel = getCurrentRelativeYPos();

		System.out.println("max memory: " + Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0);
		System.out.println("Loading the image");
		ImagePlus imp = IJ.openImage(System.getProperty("user.home") + "/HeadBack030_010um_3.tif");
		System.out.println("image loaded");
		camera = new SimulatedCamera(imp);
		// camera = new NativeCamera(0);

		URL url = getClass().getResource("/fire.lut");
		InputStream stream = url.openStream();
		IndexColorModel depthLut = LutLoader.open(stream);
		stream.close();

		adminPanel = new AdminPanel(motor.getPosition(Y_AXIS), motor.getPosition(Z_AXIS));

		mirrorQueue = new SingleElementThreadQueue();
		adminPanel.addAdminPanelListener(new AdminPanelListener() {
			@Override
			public void mirrorPositionChanged(final double pos) {
				mirrorQueue.push(new Runnable() {
					@Override
					public void run() {
						// TODO do something:
						// set mirror target pos to mirrorPos
						// wait until it's arrived
						// capture a single preview image and display it.
					}
				});
			}

			@Override
			public void done() {
				if(mode == Mode.ADMIN) {
					mode = Mode.NORMAL;
					displayWindow.remove(adminPanel);
					displayWindow.validate();
					displayPanel.requestFocusInWindow();
				}
			}
		});
		displayPanel = new PlaneDisplay(depthLut);
		displayPanel.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(acquiringStack)
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
		final byte[] frame = new byte[ICamera.WIDTH * ICamera.HEIGHT];

		buttons.addButtonsListener(new ButtonsListener() {
			@Override
			public void buttonPressed(int button) {
				System.out.println("mic: button pressed " + button);
				synchronized(Microscope.this) {
					if(acquiringStack) {
						displayPanel.requestFocusInWindow();
						return;
					}
				}

				switch(button) {
				case AbstractButtons.BUTTON_LASER:
					// TODO move mirror away and switch laser on
					break;
				case AbstractButtons.BUTTON_STACK:
					acquireStack(frame);
					break;
				case AbstractButtons.BUTTON_Y_DOWN:
					startPreview(button, Y_AXIS, false, Preferences.getStackYStart(), frame);
					break;
				case AbstractButtons.BUTTON_Y_UP:
					startPreview(button, Y_AXIS, true,  Preferences.getStackYEnd(), frame);
					break;
				case AbstractButtons.BUTTON_Z_DOWN:
					startPreview(button, Z_AXIS, false, Preferences.getStackZStart(), frame);
					break;
				case AbstractButtons.BUTTON_Z_UP:
					startPreview(button, Z_AXIS, true,  Preferences.getStackZEnd(), frame);
					break;
				}
				displayPanel.requestFocusInWindow();
			}

			@Override
			public void buttonReleased(int button) {
				switch(button) {
				case AbstractButtons.BUTTON_LASER:
					// TODO move mirror back and switch laser to triggered
					break;
				case AbstractButtons.BUTTON_STACK:
					break;
				case AbstractButtons.BUTTON_Y_DOWN:
					break;
				case AbstractButtons.BUTTON_Y_UP:
					break;
				case AbstractButtons.BUTTON_Z_DOWN:
					break;
				case AbstractButtons.BUTTON_Z_UP:
					break;
				}
				displayPanel.requestFocusInWindow();
			}
		});
	}

	public IMotor getMotor() {
		return motor;
	}

	int getCurrentPlane() {
		double zpos = motor.getPosition(Z_AXIS);
		double zrel = (zpos - Preferences.getStackZStart()) / (Preferences.getStackZEnd() - Preferences.getStackZStart());
		return (int)Math.round(zrel * ICamera.DEPTH);
	}

	double getCurrentRelativeYPos() {
		double ypos = motor.getPosition(Y_AXIS);
		return (ypos - Preferences.getStackYStart()) / (Preferences.getStackYEnd() - Preferences.getStackYStart());
	}

	void startPreview(int button, int axis, boolean positive, double target, byte[] frame) {
		synchronized(this) {
			acquiringStack = true;
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

		byte[] transmission = new byte[ICamera.WIDTH * ICamera.HEIGHT];
		for(int i = 0; i < transmission.length; i++)
			transmission[i] = 100;

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
			camera.getNextSequenceImage(frame);
			displayPanel.display(frame, transmission, yRel, plane);
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
			acquiringStack = false;
		}
	}

	void acquireStack(byte[] frame) {
		synchronized(this) {
			acquiringStack = true;
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

		displayPanel.setStackMode(true);
		displayPanel.display(null, null, yRel, ICamera.DEPTH - 1);
		motor.setTarget(Z_AXIS, Preferences.getStackZStart());

		camera.startSequence();
		for(int i = ICamera.DEPTH - 1; i >= 0; i--) {
			if(camera instanceof SimulatedCamera) {
				((SimulatedCamera) camera).setYPosition(yRel);
				((SimulatedCamera) camera).setZPosition(i);
			}
			camera.getNextSequenceImage(frame);
			displayPanel.display(frame, null, yRel, i);
		}
		camera.stopSequence();

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);

		adminPanel.setPosition(motor.getPosition(Y_AXIS), motor.getPosition(Z_AXIS));

		synchronized(this) {
			acquiringStack = false;
		}
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void shutdown() {
		while(!mirrorQueue.isIdle())
			sleep(100);
		motor.close();
		camera.close();
		buttons.close();

		mirrorQueue.shutdown();
		displayWindow.dispose();
		System.exit(0);
	}

	public static void main(String... args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					new Microscope();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
		});
	}
}
