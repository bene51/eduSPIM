package cam;

public class NativeCamera implements ICamera {

	private static native void camSetup();
	private static native void camStartPreview();
	private static native void camStopPreview();
	private static native void camGetPreviewImage(byte[] ret);
	private static native void camStartSequence();
	private static native void camGetNextSequenceImage(byte[] ret);
	private static native void camStopSequence();
	private static native void camClose();

	private boolean previewRunning = false;

	public void startPreview() {
		camStartPreview();
		previewRunning = true;
	}

	public void getPreviewImage(int z, byte[] ret) {
		camGetPreviewImage(ret);
	}

	public void stopPreview() {
		camStopPreview();
		previewRunning = false;
	}

	public boolean isPreviewRunning() {
		return previewRunning;
	}

	public void startSequence() {
		camStartSequence();
	}

	public void getNextSequenceImage(byte[] ret) {
		camGetNextSequenceImage(ret);
	}

	public void stopSequence() {
		camStopSequence();
	}

	public void close() {
		camClose();
	}
}
