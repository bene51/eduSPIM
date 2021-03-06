package cam;

import ij.ImagePlus;
import main.ExceptionHandler;

public class SimulatedCamera implements ICamera {

	private double framerate = 30;
	private double exposure = 33.3;
	private int gain = 1;

	private final ImagePlus image;
	private final byte[][] ips;
	private boolean previewOn = false;
	private int zPos;
	private int yPos;

	public SimulatedCamera(ImagePlus image) {
		this.image = image;
		if(image.getStackSize() != DEPTH && image.getStackSize() != 1)
			throw new RuntimeException("Simulated camera: Image doesn't have the correct number of planes");
		ips = new byte[DEPTH][];
		for(int z = 0; z < image.getStackSize(); z++)
			ips[z] = (byte[])image.getStack().getPixels(z + 1);
	}

	public void setZPosition(int z) {
		if(z < 0)
			zPos = 0;
		else if(z > DEPTH - 1)
			zPos = DEPTH - 1;
		else
			zPos = z;
	}

	public void setYPosition(double yRel) {
		yPos = (int)Math.round(yRel * (image.getHeight() - HEIGHT));
		if(yPos < 0)
			yPos = 0;
	}

	@Override
	public void startPreview() {
		previewOn = true;
	}

	@Override
	public void getPreviewImage(byte[] ret) {
		System.out.println("SimulatedCamera.getPreviewImage: y = " + yPos + " zPos = " + zPos);
		int y1 = Math.min(yPos + HEIGHT, image.getHeight());
		System.arraycopy(ips[DEPTH - 1 - zPos], WIDTH * yPos, ret, 0, WIDTH * (y1 - yPos));
	}

	@Override
	public void stopPreview() {
		previewOn = false;
	}

	@Override
	public boolean isPreviewRunning() {
		return previewOn;
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

		int y1 = Math.min(yPos + HEIGHT, image.getHeight());
		System.arraycopy(ips[DEPTH - 1 - zPos], WIDTH * yPos, ret, 0, WIDTH * (y1 - yPos));
		currentSequenceIndex++;
	}

	@Override
	public void stopSequence() {}

	@Override
	public double getFramerate() {
		return framerate;
	}

	@Override
	public double setFramerate(double fps) {
		framerate = fps;
		return fps;
	}

	@Override
	public double getExposuretime() {
		return exposure;
	}

	@Override
	public double setExposuretime(double exposure) {
		this.exposure = exposure;
		return exposure;
	}

	@Override
	public int getGain() {
		return gain;
	}

	@Override
	public void setGain(int gain) {
		this.gain = gain;
	}

	@Override
	public void close() {}
}
