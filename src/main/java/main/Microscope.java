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

import stage.IMotor;
import stage.SimulatedMotor;
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
 */
public class Microscope {

	private static final int COM_PORT = 7;
	private static final int BAUD_RATE = 38400;

	private static enum Mode {
		NORMAL,
		DEFINE_VOLUME_START,
		DEFINE_VOLUME_END,
		DEFINE_MIRROR_POS1,
		DEFINE_MIRROR_POS2,
	}

	private Mode mode = Mode.NORMAL;

	private boolean acquiringStack = false;

	private final IMotor motor;
	private final ICamera camera;
	private final AbstractButtons buttons;

	private final SingleElementThreadQueue mirrorQueue;

	private final PlaneDisplay displayPanel;
	private final DisplayFrame displayWindow;
	private final MirrorPanel mirrorPanel;

	// TODO whenever there occurs an exception with the camera, switch to artificial camera.
	// TODO whenever there occurs an exception with the stage, switch to artificial camera and stage.
	// TODO same for mirror once it's implemented
	// TODO acquireStack should not move the motor in y direction
	public Microscope() throws IOException { // TODO catch exception
		motor = new SimulatedMotor();
		// final IMotor motor = new NativeMotor(COM_PORT, BAUD_RATE);
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
		motor.setTarget(Y_AXIS, Preferences.getStackYStart());
		motor.setTarget(Z_AXIS, Preferences.getStackZStart());

		while(motor.isMoving())
			sleep(100);

		ImagePlus imp = IJ.openImage(System.getProperty("user.home") + "/HeadBack030_010um_2.tif");
		camera = new SimulatedCamera(imp);
		// camera = new NativeCamera(0);

		URL url = getClass().getResource("/fire.lut");
		InputStream stream = url.openStream();
		IndexColorModel depthLut = LutLoader.open(stream);
		stream.close();

		mirrorQueue = new SingleElementThreadQueue();
		mirrorPanel = new MirrorPanel();
		mirrorPanel.addMirrorPanelListener(new MirrorPanelListener() {
			public void keyPressed(KeyEvent e) {
				if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_M) {
					if(mode == Mode.DEFINE_MIRROR_POS1) {
						// TODO do something
						mode = Mode.DEFINE_MIRROR_POS2;
						displayWindow.showMessage("Go to the other end in z, adjust the mirror and press <Ctrl>-m again");
					}
					else if(mode == Mode.DEFINE_MIRROR_POS2) {
						// TODO do something
						mode = Mode.NORMAL;
						displayWindow.clearMessage();
						displayWindow.remove(mirrorPanel);
						displayWindow.doLayout();
						displayWindow.repaint();
					}
				}
				else if(e.getKeyCode() == KeyEvent.VK_ENTER ||
						e.getKeyCode() == KeyEvent.VK_UP ||
						e.getKeyCode() == KeyEvent.VK_DOWN) {
					final double mirrorPos = mirrorPanel.getPosition();
					mirrorQueue.push(new Runnable() {
						public void run() {
							// TODO do something:
							// set mirror target pos to mirrorPos
							// wait until it's arrived
							// capture a single preview image and display it.
						}
					});
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
					displayPanel.requestFocus();
					displayWindow.repaint();
				} else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_M) {
					if(mode == Mode.NORMAL) {
						displayWindow.add(mirrorPanel, BorderLayout.WEST);
						displayWindow.doLayout();
						displayWindow.repaint();
						mode = Mode.DEFINE_MIRROR_POS1;
						displayWindow.showMessage("Go to one end in z, adjust the mirror and press <Ctrl>-m again");
					}
					else if(mode == Mode.DEFINE_MIRROR_POS1) {
						// TODO do something
						mode = Mode.DEFINE_MIRROR_POS2;
						displayWindow.showMessage("Go to the other end in z, adjust the mirror and press <Ctrl>-m again");
					}
					else if(mode == Mode.DEFINE_MIRROR_POS2) {
						// TODO do something
						mode = Mode.NORMAL;
						displayWindow.clearMessage();
						displayWindow.remove(mirrorPanel);
						displayWindow.doLayout();
						displayWindow.repaint();
					}
				} else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
					if(mode == Mode.NORMAL) {
						// TODO set stack limits to motor limits
						mode = Mode.DEFINE_VOLUME_START;
						displayWindow.showMessage("Go to the y/z beginning of the volume and press <Ctrl>-c again");
					}
					else if(mode == Mode.DEFINE_VOLUME_START) {
						Preferences.setStackYStart(motor.getPosition(Y_AXIS));
						Preferences.setStackZStart(motor.getPosition(Z_AXIS));
						mode = Mode.DEFINE_VOLUME_END;
						displayWindow.showMessage("Go to the y/z end of the volume and press <Ctrl>-c again");
					}
					else if(mode == Mode.DEFINE_VOLUME_END) {
						Preferences.setStackYEnd(motor.getPosition(Y_AXIS));
						Preferences.setStackZEnd(motor.getPosition(Z_AXIS));
						mode = Mode.NORMAL;
						displayWindow.showMessage("Start: (" +
								Preferences.getStackYStart() + ", " +
								Preferences.getStackYEnd() + ")    Stop: (" +
								Preferences.getStackZStart() + ", " +
								Preferences.getStackZEnd() + ")");
						sleep(5000);
						displayWindow.clearMessage();
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
		displayWindow.setFullscreen(true);
		displayPanel.requestFocusInWindow();
		displayPanel.display(null, 0);
		final byte[] frame = new byte[ICamera.WIDTH * ICamera.HEIGHT];

		buttons.addButtonsListener(new ButtonsListener() {
			public void buttonPressed(int button) {
				displayPanel.requestFocus();
				System.out.println("mic: button pressed " + button);
				synchronized(Microscope.this) {
					if(acquiringStack) {
						System.out.println("already acquiring... returning");
						return;
					} else {
						System.out.println("Not acquiring stack");
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
					startPreview(button, Y_AXIS, Preferences.getStackYStart(), frame);
					break;
				case AbstractButtons.BUTTON_Y_UP:
					startPreview(button, Y_AXIS, Preferences.getStackYEnd(), frame);
					break;
				case AbstractButtons.BUTTON_Z_DOWN:
					startPreview(button, Z_AXIS, Preferences.getStackZStart(), frame);
					break;
				case AbstractButtons.BUTTON_Z_UP:
					startPreview(button, Z_AXIS, Preferences.getStackZEnd(), frame);
					break;
				}
			}

			public void buttonReleased(int button) {
				displayPanel.requestFocus();
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
			}
		});
	}

	int getCurrentPlane() {
		double zpos = motor.getPosition(Z_AXIS);
		double zrel = (zpos - Preferences.getStackZStart()) / (Preferences.getStackZEnd() - Preferences.getStackZStart());
		return (int)Math.round(zrel * ICamera.DEPTH);
	}

	void startPreview(int button, int axis, double target, byte[] frame) {
		synchronized(this) {
			acquiringStack = true;
		}
		System.out.println("startPreview: axis = " + axis + " target = " + target);
		// get current plane
		int plane = getCurrentPlane();
		System.out.println("start plane = " + plane);

		// set the speed of the motor according to the frame rate
		double framerate = camera.getFramerate();
		double dy = (Preferences.getStackYEnd() - Preferences.getStackYStart()) / ICamera.DEPTH;
		double dz = (Preferences.getStackZEnd() - Preferences.getStackZStart()) / ICamera.DEPTH;

		motor.setVelocity(Y_AXIS, dy * framerate);
		motor.setVelocity(Z_AXIS, dz * framerate);

		displayPanel.setStackMode(false);
		motor.setTarget(axis, target);
		camera.startPreview();

		while(motor.isMoving(axis)) {
			// stop if button was released
			if(buttons.getButtonDown() != button) {
				motor.stop();
				while(motor.isMoving())
					sleep(50);
				System.out.println("stopped motor");
				break;
			}
			if(axis == Z_AXIS)
				plane = getCurrentPlane();

			camera.getPreviewImage(plane, frame);
			displayPanel.display(frame, plane);
			System.out.println("display z = " + plane);
		}
		camera.stopPreview();

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);

		synchronized(this) {
			acquiringStack = false;
		}
	}

	void acquireStack(byte[] frame) {
		synchronized(this) {
			acquiringStack = true;
		}
		// make sure the other thread is not doing anything now
		while(camera.isPreviewRunning())
			sleep(100);

		motor.setTarget(Y_AXIS, Preferences.getStackYEnd());
		motor.setTarget(Z_AXIS, Preferences.getStackZEnd());
		displayPanel.setStackMode(false);
		while(motor.isMoving()) {
			int plane = getCurrentPlane();
			displayPanel.display(null, plane);
		}

		// set the speed of the motor according to the frame rate
		double framerate = camera.getFramerate();
		double dy = (Preferences.getStackYEnd() - Preferences.getStackYStart()) / ICamera.DEPTH;
		double dz = (Preferences.getStackZEnd() - Preferences.getStackZStart()) / ICamera.DEPTH;
		motor.setVelocity(Y_AXIS, dy * framerate);
		motor.setVelocity(Z_AXIS, dz * framerate);

		displayPanel.setStackMode(true);
		displayPanel.display(null, ICamera.DEPTH - 1);
		motor.setTarget(Y_AXIS, Preferences.getStackYStart());
		motor.setTarget(Z_AXIS, Preferences.getStackZStart());
		camera.startSequence();
		for(int i = ICamera.DEPTH - 1; i >= 0; i--) {
			camera.getNextSequenceImage(frame);
			displayPanel.display(frame, i);
		}
		camera.stopSequence();

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
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

	public static void main(String... args) throws IOException {
		new Microscope();
	}
}
