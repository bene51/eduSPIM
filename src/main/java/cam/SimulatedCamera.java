package cam;

import ij.ImagePlus;

public class SimulatedCamera implements ICamera {

	private final double FRAMERATE = 30;

	private final ImagePlus image;
	private final byte[][] ips;
	private boolean previewOn = false;
	private int zPos;
	private int yPos;

	public SimulatedCamera(ImagePlus image) {
		this.image = image;
		if(image.getStackSize() != DEPTH)
			throw new RuntimeException("Simulated camera: Image doesn't have the correct number of planes");
		ips = new byte[DEPTH][];
		for(int z = 0; z < DEPTH; z++)
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

	public void startPreview() {
		previewOn = true;
	}

	public void getPreviewImage(byte[] ret) {
		System.out.println("SimulatedCamera.getPreviewImage: y = " + yPos);
		int y1 = Math.min(yPos + HEIGHT, image.getHeight());
		System.arraycopy(ips[zPos], WIDTH * yPos, ret, 0, WIDTH * (y1 - yPos));
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

		int y1 = Math.min(yPos + HEIGHT, image.getHeight());
		System.arraycopy(ips[zPos], WIDTH * yPos, ret, 0, WIDTH * (y1 - yPos));
		currentSequenceIndex++;
	}

	public void stopSequence() {}

	public double getFramerate() {
		return FRAMERATE;
	}

	public void close() {}
}
