package cam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NativeCamera implements ICamera {

	private static native void camSetup(int camIdx) throws CameraException;
	private static native void camStartPreview(int camIdx) throws CameraException;
	private static native void camStopPreview(int camIdx) throws CameraException;
	private static native void camGetPreviewImage(int camIdx, byte[] ret) throws CameraException;
	private static native void camStartSequence(int camIdx) throws CameraException;
	private static native void camGetNextSequenceImage(int camIdx, byte[] ret) throws CameraException;
	private static native void camStopSequence(int camIdx) throws CameraException;
	private static native double camGetFramerate(int camIdx) throws CameraException;
	private static native double camSetFramerate(int camIdx, double fps) throws CameraException;
	private static native double camGetExposuretime(int camIdx) throws CameraException;
	private static native double camSetExposuretime(int camIdx, double exposure) throws CameraException;
	private static native int camGetGain(int camIdx) throws CameraException;
	private static native void camSetGain(int camIdx, int gain) throws CameraException;
	private static native void camClose(int camIdx) throws CameraException;

	private boolean previewRunning = false;
	private int camIdx;

	static {
		System.loadLibrary("cam_NativeCamera");
	}

	public NativeCamera(int camIdx, double fps, double exp, int gain) throws CameraException {
		System.out.println("NativeCamera: constructor");
		this.camIdx = camIdx;
		camSetup(camIdx);
		setFramerate(fps);
		setExposuretime(exp);
		setGain(gain);
	}

	@Override
	public void startPreview() throws CameraException {
		camStartPreview(camIdx);
		previewRunning = true;
	}

	@Override
	public void getPreviewImage(byte[] ret) throws CameraException {
		camGetPreviewImage(camIdx, ret);
	}

	@Override
	public void stopPreview() throws CameraException {
		camStopPreview(camIdx);
		previewRunning = false;
	}

	@Override
	public boolean isPreviewRunning() {
		return previewRunning;
	}

	@Override
	public void startSequence() throws CameraException {
		camStartSequence(camIdx);
	}

	@Override
	public void getNextSequenceImage(byte[] ret) throws CameraException {
		camGetNextSequenceImage(camIdx, ret);
	}

	@Override
	public void stopSequence() throws CameraException {
		camStopSequence(camIdx);
	}

	@Override
	public double getFramerate() throws CameraException {
		return camGetFramerate(camIdx);
	}

	@Override
	public double setFramerate(double fps) throws CameraException {
		return camSetFramerate(camIdx, fps);
	}

	@Override
	public double getExposuretime() throws CameraException {
		return camGetExposuretime(camIdx);
	}

	@Override
	public double setExposuretime(double exposure) throws CameraException {
		return camSetExposuretime(camIdx, exposure);
	}

	@Override
	public int getGain() throws CameraException {
		return camGetGain(camIdx);
	}

	@Override
	public void setGain(int gain) throws CameraException {
		camSetGain(camIdx, gain);
	}

	@Override
	public void close() throws CameraException {
		camClose(camIdx);
	}

	public static void main(String... args) throws IOException, CameraException {
		NativeCamera cam = new NativeCamera(0, 30, 33.3, 1);

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
			cam.getPreviewImage(frame);
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
