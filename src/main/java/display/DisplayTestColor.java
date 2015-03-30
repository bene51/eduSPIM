package display;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.LutLoader;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.IOException;

public class DisplayTestColor {

	public static void main(String[] args) throws IOException {
		ImagePlus image = IJ.openImage("/Users/bschmid/flybrain.tif");
		IndexColorModel lut = LutLoader.open("/Users/bschmid/Fiji.app/luts/fire.lut");
		DisplayTestColor test = new DisplayTestColor(image, lut);
		test.show();
	}

	private final ImagePlus image;
	private final int w, h, d;
	private final IndexColorModel[] cms;

	public DisplayTestColor(ImagePlus image, IndexColorModel depthCM) {
		this.image = image;
		this.w = image.getWidth();
		this.h = image.getHeight();
		this.d = image.getStackSize();
		this.cms = prepareColorcode(d, depthCM);
	}

	public void drawAWT(ImagePlus image, Graphics2D gr) {
		gr.setColor(Color.BLACK);
		gr.fillRect(0,  0, w, h);

		// for(int z = d - 1; z >= 0; z--) {
		for(int z = 0; z < d; z++) {
			IndexColorModel cm = cms[z];
			BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, cm);
			byte[] array = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
			// byte[] pixels = (byte[])image.getStack().getPixels(d - z);
			byte[] pixels = (byte[])image.getStack().getPixels(z + 1);
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

	private static IndexColorModel[] prepareColorcode(int nlayers, IndexColorModel lut) {
		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		lut.getReds(r);
		lut.getGreens(g);
		lut.getBlues(b);

		IndexColorModel[] colormodel = new IndexColorModel[nlayers];

		byte[] newR = new byte[256];
		byte[] newG = new byte[256];
		byte[] newB = new byte[256];
		byte[] newA = new byte[256];

		for (int j = 0; j < 256; j++)
			newA[j] = (byte)j;

		for (int i = 0; i < nlayers; i++) {
			int colorscale = (int)Math.floor((256.0 / nlayers) * i);
			for (int j = 0; j < 256; j++) {
				double intensityfactor = j / 255.0;
				newR[j] = (byte)Math.round((r[colorscale] & 0xff) * intensityfactor);
				newG[j] = (byte)Math.round((g[colorscale] & 0xff) * intensityfactor);
				newB[j] = (byte)Math.round((b[colorscale] & 0xff) * intensityfactor);
			}
			colormodel[nlayers - 1 - i] = new IndexColorModel(8, 256, newR, newG, newB, newA);
		}
		return colormodel;
	}
}
