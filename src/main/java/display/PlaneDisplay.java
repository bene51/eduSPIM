package display;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.LutLoader;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;

import cam.ICamera;

@SuppressWarnings("serial")
public class PlaneDisplay extends Canvas {

	private static final int WIDTH  = 800;
	private static final int HEIGHT = 600;

	private byte[] data = null;
	private final IndexColorModel[] cms;
	private int z = 0;

	private boolean clearGraphics = true;
	private boolean drawCoordSys = true;
	private boolean composite = true;

	private final double relativeScaleAtZEnd = 0.7;

	public PlaneDisplay(IndexColorModel lut) {
		setBackground(Color.black);
		this.cms = prepareColorcode(ICamera.DEPTH, lut);
		this.setIgnoreRepaint(true);
		// this.setDoubleBuffered(true);
	}

	public void setStackMode(boolean b) {
		if(b) {
			clearGraphics = false;
			drawCoordSys = false;
			composite = true;
		} else {
			clearGraphics = true;
			drawCoordSys = true;
			composite = false;
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(WIDTH, HEIGHT);
	}

	public void display(byte[] data, int z) {
		this.data = data;
		this.z = z;
		render();
	}

	public Image getImage() {
		BufferedImage bi = null;
		if(!composite)
			bi = new BufferedImage(ICamera.WIDTH, ICamera.HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
		else
			bi = new BufferedImage(ICamera.WIDTH, ICamera.HEIGHT, BufferedImage.TYPE_BYTE_INDEXED, cms[z]);

		byte[] array = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		System.arraycopy(data, 0, array, 0, array.length);
		return bi;
	}

	@Override
	public void update(Graphics g) {
		// paint(g);
	}

	private void drawCoordSysInt(Graphics g, double scale) {
		Graphics2D g2d = (Graphics2D)g;
		int w = getWidth();
		int h = getHeight();
		g.setColor(Color.white);
		g2d.setStroke(new BasicStroke(1f));
		for(int i = 0; i < 5; i++) {
			int z = (int)Math.round(i * ICamera.DEPTH / 4.0);
			double zScale = 1 + (relativeScaleAtZEnd - 1) * z / ICamera.DEPTH;

			int imageWidth  = 2 * (int)Math.round(ICamera.WIDTH  * scale * zScale / 2.0);
			int imageHeight = 2 * (int)Math.round(ICamera.HEIGHT * scale * zScale / 2.0);

			int xOffs = (int)Math.round((w - imageWidth) / 2.0);
			int yOffs = (int)Math.round((h - imageHeight) / 2.0);
			if(i == 0 || i == 4)
				g2d.setStroke(new BasicStroke(1f));
			else
				g2d.setStroke(new BasicStroke(0.6f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10, new float[] {3, 5}, 0));

			g.drawRect(xOffs, yOffs, imageWidth - 1, imageHeight - 1);
		}

		for(int i = 0; i < 11; i++) {
			if(i == 10)
				g2d.setStroke(new BasicStroke(1f));
			else
				g2d.setStroke(new BasicStroke(0.6f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10, new float[] {3, 5}, 0));

			double ww = i * ICamera.WIDTH / 10.0;
			int iw0 = 2 * (int)Math.round(ww  * scale / 2.0);
			int iw1 = 2 * (int)Math.round(ww  * scale / 2.0 * relativeScaleAtZEnd);
			int ih0 = 2 * (int)Math.round(ICamera.HEIGHT  * scale / 2.0);
			int ih1 = 2 * (int)Math.round(ICamera.HEIGHT  * scale / 2.0 * relativeScaleAtZEnd);
			g.drawLine((w - iw0) / 2,     (h - ih0) / 2,     (w - iw1) / 2,     (h - ih1) / 2);
			g.drawLine((w + iw0) / 2 - 1, (h - ih0) / 2,     (w + iw1) / 2 - 1, (h - ih1) / 2);
			g.drawLine((w - iw0) / 2,     (h + ih0) / 2 - 1, (w - iw1) / 2,     (h + ih1) / 2 - 1);
			g.drawLine((w + iw0) / 2 - 1, (h + ih0) / 2 - 1, (w + iw1) / 2 - 1, (h + ih1) / 2 - 1);
		}

		for(int i = 0; i < 9; i++) {
			if(i == 8)
				g2d.setStroke(new BasicStroke(1f));
			else
				g2d.setStroke(new BasicStroke(0.6f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10, new float[] {3, 5}, 0));

			double hh = i * ICamera.HEIGHT / 8.0;
			int iw0 = 2 * (int)Math.round(ICamera.WIDTH * scale / 2.0);
			int iw1 = 2 * (int)Math.round(ICamera.WIDTH * scale / 2.0 * relativeScaleAtZEnd);
			int ih0 = 2 * (int)Math.round(hh  * scale / 2.0);
			int ih1 = 2 * (int)Math.round(hh  * scale / 2.0 * relativeScaleAtZEnd);
			g.drawLine((w - iw0) / 2,     (h - ih0) / 2,     (w - iw1) / 2,     (h - ih1) / 2);
			g.drawLine((w + iw0) / 2 - 1, (h - ih0) / 2,     (w + iw1) / 2 - 1, (h - ih1) / 2);
			g.drawLine((w - iw0) / 2,     (h + ih0) / 2 - 1, (w - iw1) / 2,     (h + ih1) / 2 - 1);
			g.drawLine((w + iw0) / 2 - 1, (h + ih0) / 2 - 1, (w + iw1) / 2 - 1, (h + ih1) / 2 - 1);
		}
	}

	public void render() {
		Graphics g = getGraphics();
		if (g != null)
			paint(g);
		g.dispose();
	}

	private Image offscreenImage, renderedImage;
	private Graphics offscreenGraphics, renderedGraphics;
	private Dimension offscreenDimension;

	@Override
	public void paint(Graphics g) {
		Dimension currentSize = getSize();
		if (offscreenImage == null || renderedImage == null || !currentSize.equals(offscreenDimension)) {
			// call the 'java.awt.Component.createImage(...)' method to get an
			// image
			offscreenImage = createImage(currentSize.width, currentSize.height);
			offscreenGraphics = offscreenImage.getGraphics();
//			renderedImage = new BufferedImage(currentSize.width, currentSize.height, BufferedImage.TYPE_INT_ARGB);
			renderedImage = createImage(currentSize.width, currentSize.height);
			renderedGraphics = renderedImage.getGraphics();
			offscreenDimension = currentSize;
		}

		// rendering code here (use offscreenGraphics 'Graphics' object)
		// this algorithm assumes the background will be re-filled because it
		// reuses the image object (otherwise artifacts will
		// remain from previous renderings)
		mypaint(offscreenGraphics);

		// paint back buffer to main graphics
		g.drawImage(offscreenImage, 0, 0, this);
	}

	public void mypaint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		    RenderingHints.VALUE_ANTIALIAS_ON);

		int w = getWidth();
		int h = getHeight();

		if(clearGraphics) {
			((Graphics2D)renderedGraphics).setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
			renderedGraphics.fillRect(0, 0, w, h);
			((Graphics2D)renderedGraphics).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
		}

		g.setColor(Color.black);
		g.fillRect(0, 0, w, h);

		double sx = (double)w / ICamera.WIDTH;
		double sy = (double)h / ICamera.HEIGHT;

		double scale = Math.min(sx, sy);
		scale *= 0.8;

		if(drawCoordSys)
			drawCoordSysInt(renderedGraphics, scale);


		double zScale = 1 + (relativeScaleAtZEnd - 1) * z / ICamera.DEPTH;

		int imageWidth  = 2 * (int)Math.round(ICamera.WIDTH  * scale * zScale / 2.0);
		int imageHeight = 2 * (int)Math.round(ICamera.HEIGHT * scale * zScale / 2.0);

		int xOffs = (int)Math.round((w - imageWidth) / 2.0);
		int yOffs = (int)Math.round((h - imageHeight) / 2.0);

		if(data != null)
			renderedGraphics.drawImage(getImage(), xOffs, yOffs, imageWidth, imageHeight, null);

		g.drawImage(renderedImage, 0, 0, null);

		g.setColor(Color.red);
		g2d.setStroke(new BasicStroke(1.0f));
		g.drawRect(xOffs, yOffs, imageWidth - 1, imageHeight - 1);
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

		for (int j = 0; j < 256; j++) {
			// newA[j] = (byte)j;
			newA[j] = (byte) Math.round(255 * Math.pow(j / 255.0, 2));
		}

		for (int i = 0; i < nlayers; i++) {
			int colorscale = (int)Math.floor((256.0 / nlayers) * i);
			for (int j = 0; j < 256; j++) {
				double intensityfactor = j / 255.0;
				newR[j] = (byte)Math.round((r[colorscale] & 0xff) * intensityfactor);
				newG[j] = (byte)Math.round((g[colorscale] & 0xff) * intensityfactor);
				newB[j] = (byte)Math.round((b[colorscale] & 0xff) * intensityfactor);
			}
			colormodel[i] = new IndexColorModel(8, 256, newR, newG, newB, newA);
		}
		return colormodel;
	}

	public static void main(String... args) throws Exception {
		ImagePlus imp = IJ.openImage("/Users/bschmid/flybrain_big.tif");
		int d = imp.getStackSize();
		System.out.println(d);
		byte[][] data = new byte[d][];
		for(int z = 0; z < d; z++)
			data[z] = (byte[])imp.getStack().getPixels(z + 1);

		IndexColorModel lut = LutLoader.open("/Users/bschmid/Fiji.app/luts/fire.lut");
		PlaneDisplay disp = new PlaneDisplay(lut);
		Frame f = new Frame("bla");
		f.add(disp);
		f.pack();
		f.setVisible(true);

//		byte[] data = new byte[1280 * 1024];
//		disp.display(data, 200);

		// disp.display(data[0], 0);
		disp.clearGraphics = true;
		disp.drawCoordSys = true;
		disp.composite = false;
		for(int z = 0; z < 400; z++) {
			long start = System.currentTimeMillis();
			disp.display(data[z], z);
			long end = System.currentTimeMillis();
			System.out.println(z + ": " + (end - start));
			// Thread.sleep(10);
		}
		disp.display(null, 399);

		disp.clearGraphics = false;
		disp.drawCoordSys = false;
		disp.composite = true;
		for(int z = 399; z >= 0; z--) {
			disp.display(data[z], z);
			// Thread.sleep(10);
		}
	}
}
