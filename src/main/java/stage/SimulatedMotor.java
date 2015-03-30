package stage;

import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;


public class SimulatedMotor implements IMotor {

	public static final int DIMS = 2;

	private static final double[] POS_MIN = new double[] { 0, 0 };
	private static final double[] POS_MAX = new double[] { 15, 15 };

	private static final double[] VEL_MIN = new double[] { 0, 0 };
	private static final double[] VEL_MAX = new double[] { 0.5, 0.5 };

	private double[] pos = new double[2];
	private double[] vel = new double[2];
	private double[] tar = new double[2];

	private boolean closed = false;
	private final Object lock = new Object();
	private double epsilon = 1e-7;
	private Thread movingThread;

	public SimulatedMotor() {
		movingThread = new Thread(new MovingThread());
		movingThread.setDaemon(true);
		movingThread.start();
	}

	public int getNDimensions() {
		return DIMS;
	}

	public void getPosition(double[] pos) {
		synchronized(lock) {
			System.arraycopy(this.pos, 0, pos, 0, DIMS);
		}
		sleep(100);
	}

	public void getVelocity(double[] vel) {
		System.arraycopy(this.vel, 0, vel, 0, DIMS);
		sleep(100);
	}

	public boolean isMoving() {
		boolean moving = false;
		synchronized(lock) {
			for(int d = 0; d < DIMS; d++) {
				if(Math.abs(tar[d] - pos[d]) > epsilon) {
					moving = true;
					break;
				}
			}
		}
		System.out.println("motor.moving? " + moving);
		sleep(100);
		return moving;
	}

	public void getTarget(double[] target) {
		System.arraycopy(this.tar, 0, target, 0, DIMS);
		sleep(100);
	}

	public void getPosMin(double[] pos) {
		System.arraycopy(POS_MIN, 0, pos, 0, DIMS);
		sleep(100);
	}

	public void getPosMax(double[] pos) {
		System.arraycopy(POS_MAX, 0, pos, 0, DIMS);
		sleep(100);
	}

	public void getVelMin(double[] vel) {
		System.arraycopy(VEL_MIN, 0, vel, 0, DIMS);
		sleep(100);
	}

	public void getVelMax(double[] vel) {
		System.arraycopy(VEL_MAX, 0, vel, 0, DIMS);
		sleep(100);
	}

	public void setVelocity(double[] vel) {
		System.arraycopy(vel, 0, this.vel, 0, DIMS);
		sleep(100);
	}

	public void setTarget(double[] target) {
		System.arraycopy(target, 0, this.tar, 0, DIMS);
		sleep(100);
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		closed = true;
		try {
			movingThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private class MovingThread implements Runnable {

		long prevTimestamp = -1;
		private Clip clip;
		private boolean wasMoving = false;

		public MovingThread() {
			try {
				File file = new File(
		        		"/Users/bschmid/Downloads/Electric Motor 2-SoundBible.com-352105994.wav");
		        clip = AudioSystem.getClip();
		        // getAudioInputStream() also accepts a File or InputStream
		        AudioInputStream ais = AudioSystem.
		            getAudioInputStream( file );
		        clip.open(ais);
		        clip.loop(Clip.LOOP_CONTINUOUSLY);
		        clip.stop();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		public void run() {
			prevTimestamp = System.currentTimeMillis();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			while(!closed) {
				long timestamp = System.currentTimeMillis();
				double dt = (timestamp - prevTimestamp) / 1000.0;
				boolean moving = false;
				synchronized(lock) {
					for(int d = 0; d < DIMS; d++) {
						double diff = tar[d] - pos[d];
						if(diff != 0) {
							moving = true;
							double s = vel[d] * dt;
							if(s >= diff) {
								pos[d] = tar[d];
							} else {
								pos[d] += Math.signum(diff) * s;
							}
						}
					}
					prevTimestamp = timestamp;
				}
				if(moving && !wasMoving) {
					clip.loop(Clip.LOOP_CONTINUOUSLY);
				} else if(!moving && wasMoving) {
					clip.stop();
					clip.setFramePosition(0);
				}

				wasMoving = moving;
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main1(String[] args) {
		SimulatedMotor motor = new SimulatedMotor();
		double[] d = new double[2];
		motor.getVelMax(d);
		motor.setVelocity(d);
		d[0] = 2.5;
		d[1] = 1;
		motor.setTarget(d);
		while(motor.isMoving()) {
			motor.getPosition(d);
			System.out.println("[" + d[0] + ", " + d[1] + "]");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		motor.close();
	}

	public static void main(String[] args) throws Exception {
        File file = new File(
        		"/Users/bschmid/Downloads/Electric Motor 2-SoundBible.com-352105994.wav");
        Clip clip = AudioSystem.getClip();
        // getAudioInputStream() also accepts a File or InputStream
        AudioInputStream ais = AudioSystem.
            getAudioInputStream( file );
        clip.open(ais);
        System.out.println("nFrames: " + clip.getFrameLength());
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        Thread.sleep(6000);
        System.out.println("stop");
        clip.stop();
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                // A GUI element to prevent the Clip's daemon Thread
//                // from terminating at the end of the main()
//                JOptionPane.showMessageDialog(null, "Close to exit!");
//            }
//        });
    }
}
