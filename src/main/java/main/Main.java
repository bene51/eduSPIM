package main;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.LutLoader;

import java.awt.BorderLayout;
import java.awt.image.IndexColorModel;
import java.io.IOException;

import slider.AWTSlider;
import slider.SliderListener;
import stage.IMotor;
import stage.SimulatedMotor;
import cam.ICamera;
import cam.SimulatedCamera;
import display.DisplayFrame;
import display.PlaneDisplay;

public class Main {

	private static final double STACK_START_Z = 5;
	private static final double STACK_END_Z   = 6;
	private static final double STACK_START_X = 0;
	private static final double STACK_END_X   = 0;

	private boolean acquiringStack = false;

	public Main() throws IOException { // TODO catch exception
		final IMotor motor = new SimulatedMotor();
		double[] tmp = new double[motor.getNDimensions()];
		motor.getVelMax(tmp);
		motor.setVelocity(tmp);
		motor.setTarget(new double[] { // TODO maybe a little before? no, let's define stack_start to be early enough
				STACK_START_X,
				STACK_START_Z,
		});
		while(motor.isMoving()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		ImagePlus imp = IJ.openImage("/Users/bschmid/flybrain_big.tif");
		final ICamera camera = new SimulatedCamera(imp);

		IndexColorModel depthLut = LutLoader.open("/Users/bschmid/Fiji.app/luts/fire.lut");
		final PlaneDisplay disp = new PlaneDisplay(depthLut);
		DisplayFrame window = new DisplayFrame(disp);
		AWTSlider slider = new AWTSlider();
		window.add(slider.getScrollbar(), BorderLayout.SOUTH);
		window.pack();
		window.setVisible(true);
		final SingleElementThreadQueue sliderQueue = new SingleElementThreadQueue();
		final byte[] frame = new byte[ICamera.WIDTH * ICamera.HEIGHT];


		new Thread() {
			@Override
			public void run() {
				double[] pos = new double[motor.getNDimensions()];

				int lastPreviewPlane = -1;

				// TODO stop at some point
				while(true) {
					motor.getPosition(pos);
					double rel = (pos[1] - STACK_START_Z) / (STACK_END_Z - STACK_START_Z);
					System.out.println("rel = " + rel);
					int plane = (int)Math.round(rel * ICamera.DEPTH);
					if(plane != lastPreviewPlane)  {
						System.out.println("plane = " + plane);
						if(!camera.isPreviewRunning())
							camera.startPreview();
						camera.getPreviewImage(plane, frame);
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
//					try {
//						Thread.sleep(50);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
				}
			}
		}.start();

		// TODO close sliderQueue
		slider.addSliderListener(new SliderListener() {
			public int sliderPositionChanged(final double pos) {
				// TODO only push if not acquiring a stack
				sliderQueue.push(new Runnable() {
					public void run() {
						disp.setStackMode(false);
						double[] rwPos = new double[] {
								STACK_START_X + pos * (STACK_END_X - STACK_START_X),
								STACK_START_Z + pos * (STACK_END_Z - STACK_START_Z),
						};
						motor.setTarget(rwPos);
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
				// TODO only push if not acquiring a stack
				sliderQueue.push(new Runnable() {
					public void run() {
						acquiringStack = true;
						double[] rwPos = new double[] {
								STACK_START_X,
								STACK_START_Z,
						};
						motor.setTarget(rwPos);
						while(motor.isMoving())
							sleep(100);

						// make sure the other thread is not doing anything now
						while(camera.isPreviewRunning())
							sleep(100);

						// TODO set motor velocity according to frame rate
						disp.setStackMode(true);
						rwPos[0] = STACK_END_X;
						rwPos[1] = STACK_END_Z;
						motor.setTarget(rwPos);
						camera.startSequence();
						for(int i = 0; i < ICamera.DEPTH; i++) {
							camera.getNextSequenceImage(frame);
							disp.display(frame, i);
						}
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
