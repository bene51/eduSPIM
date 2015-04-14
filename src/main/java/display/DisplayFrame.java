package display;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

@SuppressWarnings("serial")
public class DisplayFrame extends Frame {

	private boolean fullscreen = false;

	public DisplayFrame(PlaneDisplay disp) {
		super("Display");
		add(disp);
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

	public void setFullscreen(boolean fullscreen) {
		GraphicsEnvironment env = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice device = env.getDefaultScreenDevice();

		if (fullscreen) {
			if (device.isFullScreenSupported()) {
				setVisible(false);
				dispose();
				this.setUndecorated(true);
				device.setFullScreenWindow(this);
				this.fullscreen = true;
				setVisible(true);
			}
		} else {
			setVisible(false);
			dispose();
			setUndecorated(false);
			device.setFullScreenWindow(null);
			this.fullscreen = false;
			setVisible(true);
		}
	}

//	public static void main(String[] args) throws InterruptedException {
//		byte[] data = new byte[ICamera.WIDTH * ICamera.HEIGHT];
//		PlaneDisplay disp = new PlaneDisplay();
//
//		DisplayFrame f = new DisplayFrame(disp);
//		f.pack();
//		f.setVisible(true);
//
//		for(int z = 0; z < 400; z++) {
//			disp.display(data, z);
//			Thread.sleep(5);
//		}
//		for(int z = 400; z >= 0; z--) {
//			disp.display(data, z);
//			Thread.sleep(5);
//		}
//	}
}
