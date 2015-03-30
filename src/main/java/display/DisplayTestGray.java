package display;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;

public class DisplayTestGray {

	private final ImagePlus image;
	private final int w, h, d;
	private final ImageProcessor[] ips;

	public DisplayTestGray(ImagePlus image) {
		this.image = image;
		this.w = image.getWidth();
		this.h = image.getHeight();
		this.d = image.getStackSize();
		this.ips = new ImageProcessor[d];
		for(int z = 0; z < d; z++)
			ips[z] = image.getStack().getProcessor(z + 1);
	}

	public ImageProcessor project() {
		ImageProcessor ret = ips[0].createProcessor(w, h);

		for(int y = 0, idx = 0; y < h; y++) {
			for(int x = 0; x < w; x++, idx++) {
				float color = 0;
				for(int z = d - 1; z >= 0; z--) {
					float v = ips[z].getf(idx);
					double alphai = v / 255f;
					if(alphai > 0)
						color = (float)(color * (1 - alphai) + v * alphai);
				}
				ret.setf(idx, color);
			}
		}
		return ret;
	}

	public void drawIJ(ImagePlus image, Graphics2D gr) {
		ImageProcessor ip = project();
		gr.drawImage(ip.createImage(), 0, 0, w, h, null);
	}

	public void drawAWT(ImagePlus image, Graphics2D gr) {

		gr.setColor(Color.BLACK);
		gr.fillRect(0,  0, w, h);

		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		byte[] a = new byte[256];
		for(int i=0; i<256; i++) {
			r[i]=(byte)i;
			g[i]=(byte)i;
			b[i]=(byte)i;
			a[i]=(byte)i;
		}
		IndexColorModel cm = new IndexColorModel(8, 256, r, g, b, a);

		for(int z = d - 1; z >= 0; z--) {
			BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, cm);
			byte[] array = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
			byte[] pixels = (byte[])ips[z].getPixels();
			System.arraycopy(pixels, 0, array, 0, array.length);
			gr.drawImage(bi, 0, 0, w, h, null);
		}
	}

	public void show() {
		@SuppressWarnings("serial")
		final Panel p = new Panel() {
			@Override
			public void paint(Graphics g) {
				long start = System.currentTimeMillis();
				for(int i = 0; i < 500; i++)
					drawAWT(image, (Graphics2D)g);
				long end = System.currentTimeMillis();
				System.out.println("took " + (end - start) + " ms");
			}
		};
		p.setPreferredSize(new Dimension(w, h));
		Frame frame = new Frame("Display");
		frame.add(p);
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		ImagePlus image = IJ.openImage("/Users/bschmid/flybrain.tif");
		DisplayTestGray test = new DisplayTestGray(image);
		test.show();
	}
}
