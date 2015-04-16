package cam;

import ij.ImagePlus;

public class SimulatedCamera implements ICamera {

	private final double FRAMERATE = 30;

	private ImagePlus image;
	private boolean previewOn = false;

	public SimulatedCamera(ImagePlus image) {
		this.image = image;
		if(image.getStackSize() != DEPTH)
			throw new RuntimeException("Simulated camera: Image doesn't have the correct number of planes");
	}

	public void startPreview() {
		previewOn = true;
	}

	public void getPreviewImage(int z, byte[] ret) {
		System.arraycopy(image.getStack().getPixels(z + 1), 0, ret, 0, WIDTH * HEIGHT);
	}

	public void stopPreview() {
		previewOn = false;
	}

	public boolean isPreviewRunning() {
		return previewOn;
	}

	private int currentSequenceIndex = 0;

	public void startSequence() {
		currentSequenceIndex = 0;
	}

	private long sequenceStartTime = 0;

	public void getNextSequenceImage(byte[] ret) {
		long time = System.currentTimeMillis();
		if(currentSequenceIndex == 0) {
			sequenceStartTime = time;
		} else {
			long targetTime = (long)(sequenceStartTime + currentSequenceIndex * 1000 / FRAMERATE);
			if(targetTime > time) {
				try {
					Thread.sleep(targetTime - time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		System.arraycopy(image.getStack().getPixels(DEPTH - currentSequenceIndex), 0, ret, 0, WIDTH * HEIGHT);
		currentSequenceIndex++;
	}

	public void stopSequence() {}

	public double getFramerate() {
		return FRAMERATE;
	}

	public void close() {}
}
