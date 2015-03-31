package main;

import static stage.IMotor.Y_AXIS;
import static stage.IMotor.Z_AXIS;
import ij.plugin.LutLoader;

import java.awt.BorderLayout;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import slider.AWTSlider;
import slider.SliderListener;
import stage.IMotor;
import stage.SimulatedMotor;
import cam.ICamera;
import cam.NativeCamera;
import display.DisplayFrame;
import display.PlaneDisplay;

public class Main {

	private static final double STACK_START_Z = 5;
	private static final double STACK_END_Z   = 6;
	private static final double STACK_START_Y = 0;
	private static final double STACK_END_Y   = 0;

	private boolean acquiringStack = false;

	public Main() throws IOException { // TODO catch exception
		final IMotor motor = new SimulatedMotor();
		// TODO close motor at some point
		motor.setVelocity(Y_AXIS, IMotor.VEL_MAX_Y);
		motor.setVelocity(Z_AXIS, IMotor.VEL_MAX_Z);
		motor.setTarget(Y_AXIS, STACK_START_Y);
		motor.setTarget(Z_AXIS, STACK_START_Z);

		while(motor.isMoving()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// ImagePlus imp = IJ.openImage("/Users/bschmid/flybrain_big.tif");
		final ICamera camera = new NativeCamera(0); // new SimulatedCamera(imp);
		// TODO close camera at some point

		URL url = getClass().getResource("/fire.lut");
		InputStream stream = url.openStream();
		IndexColorModel depthLut = LutLoader.open(stream);
		stream.close();
		final PlaneDisplay disp = new PlaneDisplay(depthLut);
		DisplayFrame window = new DisplayFrame(disp);
		final AWTSlider slider = new AWTSlider();
		window.add(slider.getScrollbar(), BorderLayout.SOUTH);
		window.pack();
		window.setVisible(true);
		final SingleElementThreadQueue sliderQueue = new SingleElementThreadQueue();
		final byte[] frame = new byte[ICamera.WIDTH * ICamera.HEIGHT];


		new Thread() {
			@Override
			public void run() {

				int lastPreviewPlane = -1;

				// TODO stop at some point
				while(true) {
					double pos = motor.getPosition(Z_AXIS);
					double rel = (pos - STACK_START_Z) / (STACK_END_Z - STACK_START_Z);
					System.out.println("rel = " + rel);
					int plane = (int)Math.round(rel * ICamera.DEPTH);
					if(plane != lastPreviewPlane)  {
						System.out.println("plane = " + plane);
						if(!camera.isPreviewRunning())
							camera.startPreview();
						System.out.println("before getPreviewImage");
						camera.getPreviewImage(plane, frame);
						System.out.println("after getPreviewImage");
						System.out.println("About to draw new preview image");
						disp.display(frame, plane);
						lastPreviewPlane = plane;
					}

					if(!motor.isMoving() && sliderQueue.isIdle() || acquiringStack) {
						if(camera.isPreviewRunning()) {
							camera.stopPreview();
						} else {
							System.out.println("preview not running");
						}
						synchronized(Main.this) {
							try {
								Main.this.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					} else {
						System.out.println("sliderQueue not idle");
					}
				}
			}
		}.start();

		// TODO close sliderQueue
		slider.addSliderListener(new SliderListener() {
			public int sliderPositionChanged(final double pos) {
				// only push if not acquiring a stack
				if(acquiringStack)
					return 0;
				sliderQueue.push(new Runnable() {
					public void run() {
						disp.setStackMode(false);
						motor.setTarget(Y_AXIS, STACK_START_Y + pos * (STACK_END_Y - STACK_START_Y));
						motor.setTarget(Z_AXIS, STACK_START_Z + pos * (STACK_END_Z - STACK_START_Z));
						synchronized(Main.this) {
							Main.this.notifyAll();
						}
						System.out.println("sliderPositionChanged(" + pos + ")");
					}
				});
				return 0;
			}

			public int sliderReleased(double startPos) {
				System.out.println("sliderPositionReleased(" + startPos + ")");
				// only push if not acquiring a stack
				if(acquiringStack)
					return 0;
				sliderQueue.push(new Runnable() {
					public void run() {
						acquiringStack = true;
						// make sure the other thread is not doing anything now
						while(camera.isPreviewRunning())
							sleep(100);

						motor.setTarget(Y_AXIS, STACK_END_Y);
						motor.setTarget(Z_AXIS, STACK_END_Z);
						while(motor.isMoving()) {
							double pos = motor.getPosition(Z_AXIS);
							double rel = (pos - STACK_START_Z) / (STACK_END_Z - STACK_START_Z);
							int plane = (int)Math.round(rel * ICamera.DEPTH);
							disp.display(null, plane);
						}

						// TODO set motor velocity according to frame rate
						disp.setStackMode(true);
						disp.display(null, ICamera.DEPTH - 1);
						motor.setTarget(Y_AXIS, STACK_START_Y);
						motor.setTarget(Z_AXIS, STACK_START_Z);
						camera.startSequence();
						for(int i = ICamera.DEPTH - 1; i >= 0; i--) {
							camera.getNextSequenceImage(frame);
							disp.display(frame, i);
						}
						camera.stopSequence();
						acquiringStack = false;
						slider.setPosition(0);
					}
				});
				return 0;
			}
		});
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String... args) throws IOException {
		new Main();
	}
}
