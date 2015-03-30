package display;

import java.awt.Frame;

@SuppressWarnings("serial")
public class DisplayFrame extends Frame {

	public DisplayFrame(PlaneDisplay disp) {
		super("Display");
		add(disp);
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
