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


	public void startPreview() throws CameraException;

	public void getPreviewImage(byte[] ret) throws CameraException;

	public void stopPreview() throws CameraException;

	public boolean isPreviewRunning();

	public void startSequence() throws CameraException;

	public void getNextSequenceImage(byte[] ret) throws CameraException;

	public void stopSequence() throws CameraException;

	public double getFramerate() throws CameraException;

	public void close() throws CameraException;
}
