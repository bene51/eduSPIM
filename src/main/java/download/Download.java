package download;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.AVI_Reader;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Observable;

// This class downloads a file from a URL.
public class Download extends Observable implements Runnable {

	// Max size of download buffer.
	private static final int MAX_BUFFER_SIZE = 1024;

	// These are the status names.
	public static final String STATUSES[] = {
		"Downloading",
		"Converting",
		"Complete",
		"Saving",
		"Error"
	};

	// These are the status codes.
	public static final int DOWNLOADING = 0;
	public static final int CONVERTING = 1;
	public static final int COMPLETE = 2;
	public static final int SAVING = 3;
	public static final int ERROR = 4;

	private URL url; // download URL
	private String downloadPath; // output path
	private String convertPath;
	private int size; // size of download in bytes
	private int downloaded; // number of bytes downloaded
	private int status; // current status of download
	private ImagePlus image;

	// Constructor for Download.
	public Download(URL url, String downloadPath, String convertPath) {
		this.url = url;
		this.downloadPath = downloadPath;
		this.convertPath = convertPath;
		size = -1;
		downloaded = 0;
		status = DOWNLOADING;

		// Begin the download.
		download();
	}

	public ImagePlus getImage() {
		return image;
	}

	// Get this download's URL.
	public String getUrl() {
		return url.toString();
	}

	// Get this download's size.
	public int getSize() {
		return size;
	}

	// Get this download's progress.
	public float getProgress() {
		return ((float) downloaded / size) * 100;
	}

	// Get this download's status.
	public int getStatus() {
		return status;
	}

	// Mark this download as having an error.
	private void error() {
		status = ERROR;
		stateChanged();
	}

	// Start or resume downloading.
	private void download() {
		Thread thread = new Thread(this);
		thread.start();
	}

	private void convert() {
		ImagePlus imp = AVI_Reader.open(downloadPath, true);
		ImageStack stack = new ImageStack(imp.getWidth(), imp.getHeight());
		int d = imp.getStackSize();
		downloaded = 0;
		stateChanged();
		for(int z = 0; z < d; z++) {
			stack.addSlice(imp.getStack().getProcessor(z + 1).convertToByte(false));
			downloaded += size / d;
			stateChanged();
		}
		this.image = new ImagePlus(new File(downloadPath).getName(), stack);
	}

	private void export() {
		IJ.save(image, convertPath);
	}

	// Download file.
	@Override
	public void run() {
		RandomAccessFile file = null;
		InputStream stream = null;

		try {
			// Open connection to URL.
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();

			// Specify what portion of file to download.
			connection.setRequestProperty("Range", "bytes=" + downloaded + "-");

			// Connect to server.
			connection.connect();

			// Make sure response code is in the 200 range.
			if (connection.getResponseCode() / 100 != 2) {
				error();
			}

			// Check for valid content length.
			int contentLength = connection.getContentLength();
			if (contentLength < 1) {
				error();
			}

			/*
			 * Set the size for this download if it hasn't been already set.
			 */
			if (size == -1) {
				size = contentLength;
				stateChanged();
			}

			// Open file and seek to the end of it.
			file = new RandomAccessFile(downloadPath, "rw");
			file.seek(downloaded);

			stream = connection.getInputStream();
			while (status == DOWNLOADING) {
				/*
				 * Size buffer according to how much of the file is left to
				 * download.
				 */
				byte buffer[];
				if (size - downloaded > MAX_BUFFER_SIZE) {
					buffer = new byte[MAX_BUFFER_SIZE];
				} else {
					buffer = new byte[size - downloaded];
				}

				// Read from server into buffer.
				int read = stream.read(buffer);
				if (read == -1)
					break;

				// Write buffer to file.
				file.write(buffer, 0, read);
				downloaded += read;
				stateChanged();
			}

			/*
			 * Change status to complete if this point was reached because
			 * downloading has finished.
			 */
			if (status == DOWNLOADING) {
				status = CONVERTING;
				stateChanged();
			}

			convert();
			status = SAVING;
			stateChanged();

			export();
			status = COMPLETE;
			stateChanged();
		} catch (Exception e) {
			error();
		} finally {
			// Close file.
			if (file != null) {
				try {
					file.close();
				} catch (Exception e) {
				}
			}

			// Close connection to server.
			if (stream != null) {
				try {
					stream.close();
				} catch (Exception e) {
				}
			}
		}
	}

	// Notify observers that this download's status has changed.
	private void stateChanged() {
		setChanged();
		notifyObservers();
	}
}