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

	public void getNextSequenceImage(byte[] ret) {
		System.arraycopy(image.getStack().getPixels(currentSequenceIndex + 1), 0, ret, 0, WIDTH * HEIGHT);
		currentSequenceIndex++;
	}

	public void stopSequence() {}

	public double getFramerate() {
		return FRAMERATE;
	}

	public void close() {}
}
