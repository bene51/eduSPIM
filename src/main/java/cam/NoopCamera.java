package cam;

import main.ExceptionHandler;

public class NoopCamera implements ICamera {

	private boolean previewRunning = false;
	private double framerate = 30;
	private double exposure = 50;
	private int gain = 1;

	@Override
	public void startPreview() throws CameraException {
		previewRunning = true;
	}

	@Override
	public void getPreviewImage(byte[] ret) throws CameraException {}

	@Override
	public void stopPreview() throws CameraException {}

	@Override
	public boolean isPreviewRunning() {
		return previewRunning;
	}

	private int currentSequenceIndex = 0;

	@Override
	public void startSequence() {
		currentSequenceIndex = 0;
	}

	private long sequenceStartTime = 0;

	@Override
	public void getNextSequenceImage(byte[] ret) {
		long time = System.currentTimeMillis();
		if(currentSequenceIndex == 0) {
			sequenceStartTime = time;
		} else {
			long targetTime = (long)(sequenceStartTime + currentSequenceIndex * 1000 / framerate);
			if(targetTime > time) {
				try {
					Thread.sleep(targetTime - time);
				} catch (InterruptedException e) {
					ExceptionHandler.handleException("Interrupted during artificial delay", e);
				}
			}
		}

		currentSequenceIndex++;
	}

	@Override
	public void stopSequence() throws CameraException {}

	@Override
	public double getFramerate() throws CameraException {
		return framerate;
	}

	@Override
	public double setFramerate(double fps) throws CameraException {
		this.framerate = fps;
		return framerate;
	}

	@Override
	public double getExposuretime() throws CameraException {
		return exposure;
	}

	@Override
	public double setExposuretime(double exposure) throws CameraException {
		this.exposure = exposure;
		return exposure;
	}

	@Override
	public int getGain() throws CameraException {
		return gain;
	}

	@Override
	public void setGain(int gain) throws CameraException {
		this.gain = gain;
	}

	@Override
	public void close() throws CameraException {}
}
