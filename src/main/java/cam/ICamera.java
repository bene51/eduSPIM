package cam;

/*
 * Constructor: setup camera:
 *              - field of view
 *              - exposure time / framerate
 *              - pixel depth
 *              - memory buffer for XX frames
 *
 *
 * startPreview():
 *              - run camera in freerunning mode and use
 *                the buffer as ring buffer which is
 *                continuously overwritten
 *
 * getPreviewImage():
 *              - get the latest image that's been acquired
 *
 * stopPreview():
 *
 * startSequence(n):
 *              - acquire n consecutive frames (n <= buffersize)
 *
 * getFrame(i):
 *              - get the ith frame of an acquisition started with 'acquire()'
 *
 * stopSequence():
 *
 * isSequenceRunning();
 * isPreviewRunning();
 *
 */
public interface ICamera {

	public final int DEPTH  = 100;
	public final int WIDTH  = 1280;
	public final int HEIGHT = 1024;


	public void startPreview();

	/**
	 * z is only needed for the simulated camera.
	 * @param z
	 * @param ret
	 * @return
	 */
	public void getPreviewImage(int z, byte[] ret);

	public void stopPreview();

	public boolean isPreviewRunning();

	public void startSequence();

	public void getNextSequenceImage(byte[] ret);

	public void stopSequence();

	public void close();
}
