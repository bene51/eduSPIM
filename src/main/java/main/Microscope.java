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

	private static final double STACK_START_Z = 5;
	private static final double STACK_END_Z   = 6;
	private static final double STACK_DZ      = 0.01; // in motor units (i.e. mm)
	private static final double STACK_START_Y = 0;
	private static final double STACK_END_Y   = 0;
	private static final double STACK_DY      = 0;

	private boolean acquiringStack = false;
//	private boolean shutdown = false;

	private final IMotor motor;
	private final ICamera camera;
	// private final AWTSlider slider;
	private final AbstractButtons buttons;

	private final SingleElementThreadQueue sliderQueue;

	private final PlaneDisplay displayPanel;
	private final DisplayFrame displayWindow;

	// TODO whenever there occurs an exception with the camera, switch to artificial camera.
	// TODO whenever there occurs an exception with the stage, switch to artificial camera and stage.
	// TODO same for mirror once it's implemented
	public Microscope() throws IOException { // TODO catch exception
		motor = new SimulatedMotor();
		// final IMotor motor = new NativeMotor(COM_PORT, BAUD_RATE);
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
		motor.setTarget(Y_AXIS, STACK_START_Y);
		motor.setTarget(Z_AXIS, STACK_START_Z);

		while(motor.isMoving())
			sleep(100);

		ImagePlus imp = IJ.openImage(System.getProperty("user.home") + "/HeadBack030_010um_2.tif");
		camera = new SimulatedCamera(imp);
		// camera = new NativeCamera(0);

		URL url = getClass().getResource("/fire.lut");
		InputStream stream = url.openStream();
		IndexColorModel depthLut = LutLoader.open(stream);
		stream.close();
		displayPanel = new PlaneDisplay(depthLut);
		displayPanel.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Q) {
					shutdown();
				} else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_F) {
					boolean fs = !displayWindow.isFullscreen();
					System.out.println("put window to fullscreen: " + fs);
					displayWindow.setFullscreen(fs);
					displayPanel.requestFocus();
					displayWindow.repaint();
					displayPanel.render();
				}
			}
		});
		displayWindow = new DisplayFrame(displayPanel);
		// slider = new AWTSlider();
		// displayWindow.add(slider.getScrollbar(), BorderLayout.SOUTH);
		buttons = new AWTButtons();
		if(buttons instanceof AWTButtons)
			displayWindow.add(((AWTButtons)buttons).getPanel(), BorderLayout.EAST);
		displayWindow.pack();
		displayWindow.setVisible(true);
		displayWindow.setFullscreen(true);
		displayPanel.requestFocusInWindow();
		sliderQueue = new SingleElementThreadQueue();
		final byte[] frame = new byte[ICamera.WIDTH * ICamera.HEIGHT];


//		new Thread() {
//			@Override
//			public void run() {
//
//				int lastPreviewPlane = -1;
//
//				while(!shutdown) {
//					double pos = motor.getPosition(Z_AXIS);
//					double rel = (pos - STACK_START_Z) / (STACK_END_Z - STACK_START_Z);
//					System.out.println("rel = " + rel);
//					int plane = (int)Math.round(rel * ICamera.DEPTH);
//					if(plane != lastPreviewPlane)  {
//						System.out.println("plane = " + plane);
//						if(!camera.isPreviewRunning())
//							camera.startPreview();
//						System.out.println("before getPreviewImage");
//						camera.getPreviewImage(plane, frame);
//						System.out.println("after getPreviewImage");
//						System.out.println("About to draw new preview image");
//						displayPanel.display(frame, plane);
//						lastPreviewPlane = plane;
//					}
//
//					if(!motor.isMoving() && sliderQueue.isIdle() || acquiringStack) {
//						if(camera.isPreviewRunning()) {
//							camera.stopPreview();
//						} else {
//							System.out.println("preview not running");
//						}
//						synchronized(Microscope.this) {
//							try {
//								Microscope.this.wait();
//							} catch (InterruptedException e) {
//								e.printStackTrace();
//							}
//						}
//					} else {
//						System.out.println("sliderQueue not idle");
//					}
//				}
//			}
//		}.start();

		buttons.addButtonsListener(new ButtonsListener() {
			public void buttonPressed(int button) {
				if(acquiringStack)
					return;

				switch(button) {
				case AbstractButtons.BUTTON_LASER:
					// TODO move mirror away and switch laser on
					break;
				case AbstractButtons.BUTTON_STACK:
					acquireStack(frame);
					break;
				case AbstractButtons.BUTTON_Y_DOWN:
					startPreview(button, Y_AXIS, STACK_START_Y, frame);
					break;
				case AbstractButtons.BUTTON_Y_UP:
					startPreview(button, Y_AXIS, STACK_END_Y, frame);
					break;
				case AbstractButtons.BUTTON_Z_DOWN:
					startPreview(button, Z_AXIS, STACK_START_Z, frame);
					break;
				case AbstractButtons.BUTTON_Z_UP:
					startPreview(button, Z_AXIS, STACK_END_Z, frame);
					break;
				}
			}

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
			}
		});

//		slider.addSliderListener(new SliderListener() {
//			public int sliderPositionChanged(final double pos) {
//				// only push if not acquiring a stack
//				if(acquiringStack)
//					return 0;
//				sliderQueue.push(new Runnable() {
//					public void run() {
//						displayPanel.setStackMode(false);
//						motor.setTarget(Y_AXIS, STACK_START_Y + pos * (STACK_END_Y - STACK_START_Y));
//						motor.setTarget(Z_AXIS, STACK_START_Z + pos * (STACK_END_Z - STACK_START_Z));
//						synchronized(Microscope.this) {
//							Microscope.this.notifyAll();
//						}
//						System.out.println("sliderPositionChanged(" + pos + ")");
//					}
//				});
//				displayPanel.requestFocusInWindow();
//				return 0;
//			}
//
//			public int sliderReleased(double startPos) {
//				System.out.println("sliderPositionReleased(" + startPos + ")");
//				// only push if not acquiring a stack
//				if(acquiringStack)
//					return 0;
//				sliderQueue.push(new Runnable() {
//					public void run() {
//						acquireStack(frame);
//						slider.setPosition(0);
//					}
//				});
//				displayPanel.requestFocusInWindow();
//				return 0;
//			}
//		});
	}

	void startPreview(int button, int axis, double target, byte[] frame) {
		System.out.println("startPreview: axis = " + axis + " target = " + target);
		// get current plane
		double zpos = motor.getPosition(Z_AXIS);
		double zrel = (zpos - STACK_START_Z) / (STACK_END_Z - STACK_START_Z);
		int plane = (int)Math.round(zrel * ICamera.DEPTH);
		System.out.println("start plane = " + plane);

		// set the speed of the motor according to the frame rate
		double framerate = camera.getFramerate();
		motor.setVelocity(Y_AXIS, STACK_DY * framerate);
		motor.setVelocity(Z_AXIS, STACK_DZ * framerate);

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
			if(axis == Z_AXIS) {
				zpos = motor.getPosition(Z_AXIS);
				zrel = (zpos - STACK_START_Z) / (STACK_END_Z - STACK_START_Z);
				plane = (int)Math.round(zrel * ICamera.DEPTH);
			}
			camera.getPreviewImage(plane, frame);
			displayPanel.display(frame, plane);
			System.out.println("display z = " + plane);
		}
		camera.stopPreview();

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);

		acquiringStack = false;
	}

	void acquireStack(byte[] frame) {
		acquiringStack = true;
		// make sure the other thread is not doing anything now
		while(camera.isPreviewRunning())
			sleep(100);

		motor.setTarget(Y_AXIS, STACK_END_Y);
		motor.setTarget(Z_AXIS, STACK_END_Z);
		displayPanel.setStackMode(false);
		while(motor.isMoving()) {
			double pos = motor.getPosition(Z_AXIS);
			double rel = (pos - STACK_START_Z) / (STACK_END_Z - STACK_START_Z);
			int plane = (int)Math.round(rel * ICamera.DEPTH);
			displayPanel.display(null, plane);
		}

		// set the speed of the motor according to the frame rate
		double framerate = camera.getFramerate();
		motor.setVelocity(Y_AXIS, STACK_DY * framerate);
		motor.setVelocity(Z_AXIS, STACK_DZ * framerate);

		displayPanel.setStackMode(true);
		displayPanel.display(null, ICamera.DEPTH - 1);
		motor.setTarget(Y_AXIS, STACK_START_Y);
		motor.setTarget(Z_AXIS, STACK_START_Z);
		camera.startSequence();
		for(int i = ICamera.DEPTH - 1; i >= 0; i--) {
			camera.getNextSequenceImage(frame);
			displayPanel.display(frame, i);
		}
		camera.stopSequence();
		acquiringStack = false;

		// reset the motor speed
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void shutdown() {
//		shutdown = true;
		while(!sliderQueue.isIdle())
			sleep(100);
		motor.close();
		camera.close();
//		slider.close();
		buttons.close();

		sliderQueue.shutdown();
		displayWindow.dispose();
		System.exit(0);
	}

	public static void main(String... args) throws IOException {
		new Microscope();
	}
}
