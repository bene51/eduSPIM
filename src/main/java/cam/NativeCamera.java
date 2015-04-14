package cam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NativeCamera implements ICamera {

	private static native void camSetup(int camIdx);
	private static native void camStartPreview(int camIdx);
	private static native void camStopPreview(int camIdx);
	private static native void camGetPreviewImage(int camIdx, byte[] ret);
	private static native void camStartSequence(int camIdx);
	private static native void camGetNextSequenceImage(int camIdx, byte[] ret);
	private static native void camStopSequence(int camIdx);
	private static native double camGetFramerate(int camIdx);
	private static native void camClose(int camIdx);

	private boolean previewRunning = false;
	private int camIdx;

	static {
		System.loadLibrary("cam_NativeCamera");
	}

	public NativeCamera(int camIdx) {
		System.out.println("NativeCamera: constructor");
		camSetup(camIdx);
	}

	public void startPreview() {
		camStartPreview(camIdx);
		previewRunning = true;
	}

	public void getPreviewImage(int z, byte[] ret) {
		camGetPreviewImage(camIdx, ret);
	}

	public void stopPreview() {
		camStopPreview(camIdx);
		previewRunning = false;
	}

	public boolean isPreviewRunning() {
		return previewRunning;
	}

	public void startSequence() {
		camStartSequence(camIdx);
	}

	public void getNextSequenceImage(byte[] ret) {
		camGetNextSequenceImage(camIdx, ret);
	}

	public void stopSequence() {
		camStopSequence(camIdx);
	}

	public double getFramerate() {
		return camGetFramerate(camIdx);
	}

	public void close() {
		camClose(camIdx);
	}

	public static void main(String... args) throws IOException {
		NativeCamera cam = new NativeCamera(0);

		byte[] frame = new byte[WIDTH * HEIGHT];

		// acquire a sequence
		cam.startSequence();
		for (int i = 0; i < DEPTH; i++) {
			cam.getNextSequenceImage(frame);
		}
		cam.stopSequence();

		// acquire a sequence
		cam.startSequence();
		for (int i = 0; i < DEPTH; i++) {
			cam.getNextSequenceImage(frame);
		}
		cam.stopSequence();

		// test preview
		cam.startPreview();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line;
		while (!(line = in.readLine()).equals("quit")) {
			System.out.println("echo: " + line);
			cam.getPreviewImage(0, frame);
			System.out.println(Integer.toString(frame[0]));
		}
		System.out.println("echo: " + line);
		cam.stopPreview();

//		// acquire a sequence
//		cam.startSequence();
//		for (int i = 0; i < 10; i++) {
//			cam.getNextSequenceImage(frame);
//		}
//		cam.stopSequence();

		cam.close();
	}
}
