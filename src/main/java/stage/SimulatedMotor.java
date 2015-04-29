package stage;

import java.io.InputStream;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import main.ExceptionHandler;


public class SimulatedMotor implements IMotor {

	public static final int DIMS = 2;

	private double[] pos = new double[2];
	private double[] vel = new double[2];
	private double[] tar = new double[2];

	private boolean closed = false;
	private final Object lock = new Object();
	private double epsilon = 1e-7;
	private Thread movingThread;
	private boolean stopMoving = false;

	public SimulatedMotor() {
		movingThread = new Thread(new MovingThread());
		movingThread.setDaemon(true);
		movingThread.start();
	}

	@Override
	public double getPosition(int axis) {
		double ret = 0;
		synchronized(lock) {
			ret = pos[axis];
		}
		sleep(50);
		return ret;
	}

	@Override
	public double getVelocity(int axis) {
		sleep(50);
		return vel[axis];
	}

	@Override
	public boolean isMoving(int axis) {
		boolean moving = false;
		synchronized(lock) {
			moving = Math.abs(tar[axis] - pos[axis]) > epsilon;
		}
		sleep(50);
		return moving;
	}

	@Override
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
		System.out.println("motor.moving? " + moving + " (" + pos[0] + ", " + pos[1] + ")");
		sleep(100);
		return moving;
	}

	@Override
	public void setVelocity(int axis, double vel) {
		this.vel[axis] = vel;
		sleep(50);
	}

	@Override
	public void setTarget(int axis, double target) {
		this.tar[axis] = target;
		sleep(50);
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch(InterruptedException e) {
			ExceptionHandler.handleException("Interrupted during artificial delay", e);
		}
	}

	@Override
	public void stop() {
		stopMoving = true;
	}

	@Override
	public void close() {
		closed = true;
		try {
			movingThread.join();
		} catch (InterruptedException e) {
			ExceptionHandler.handleException("Error closing the motor", e);
		}
	}

	private class MovingThread implements Runnable {

		long prevTimestamp = -1;
		private Clip clip;
		private boolean wasMoving = false;

		public MovingThread() {
			try {
				URL url = getClass().getResource("/MotorSound.wav");
				InputStream stream = url.openStream();
		        clip = AudioSystem.getClip();
		        AudioInputStream ais = AudioSystem.getAudioInputStream(stream);
		        clip.open(ais);
		        clip.loop(Clip.LOOP_CONTINUOUSLY);
		        clip.stop();
			} catch(Exception e) {
				ExceptionHandler.handleException("Error initializing the artificial motor sound", e);
			}
		}

		@Override
		public void run() {
			try {
				dorun();
			} catch(Throwable e) {
				ExceptionHandler.handleException("Unexpected exception in SimulatedMotor", e);
			}
		}

		public void dorun() {
			prevTimestamp = System.currentTimeMillis();
			sleep(50);
			while(!closed) {
				long timestamp = System.currentTimeMillis();
				double dt = (timestamp - prevTimestamp) / 1000.0;
				prevTimestamp = timestamp;
				boolean moving = false;
				synchronized(lock) {
					if(stopMoving) {
						for(int d = 0; d < DIMS; d++)
							tar[d] = pos[d];
						stopMoving = false;
					}

					for(int d = 0; d < DIMS; d++) {
						double diff = tar[d] - pos[d];
						if(diff != 0) {
							moving = true;
							double s = vel[d] * dt;
							if(s >= Math.abs(diff)) {
								pos[d] = tar[d];
							} else {
								pos[d] += Math.signum(diff) * s;
							}
						}
					}
				}

				if(moving && !wasMoving && clip != null) {
					clip.loop(Clip.LOOP_CONTINUOUSLY);
				} else if(!moving && wasMoving && clip != null) {
					clip.stop();
					clip.setFramePosition(0);
				}

				wasMoving = moving;
				sleep(50);
			}
		}
	}

	public static void main1() {
		SimulatedMotor motor = new SimulatedMotor();
		motor.setVelocity(IMotor.Y_AXIS, IMotor.POS_MAX_Y);
		motor.setVelocity(IMotor.Z_AXIS, IMotor.POS_MAX_Z);
		motor.setTarget(IMotor.Y_AXIS, 2.5);
		motor.setTarget(IMotor.Z_AXIS, 1);
		while(motor.isMoving()) {
			double y = motor.getPosition(IMotor.Y_AXIS);
			double z = motor.getPosition(IMotor.Z_AXIS);
			System.out.println("[" + y + ", " + z + "]");
		}
		motor.close();
	}

	public static void main(String[] args) throws Exception {
		URL url = SimulatedMotor.class.getResource("/MotorSound.wav");
		InputStream stream = url.openStream();
        Clip clip = AudioSystem.getClip();
        AudioInputStream ais = AudioSystem.getAudioInputStream(stream);
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
