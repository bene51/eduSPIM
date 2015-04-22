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
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;

import main.Preferences;
import cam.ICamera;

@SuppressWarnings("serial")
public class PlaneDisplay extends Canvas {

	private static final int WIDTH  = 800;
	private static final int HEIGHT = 600;

	private byte[] fluorescence = null;
	private byte[] transmission = null;
	private final IndexColorModel[] stackColorModels;
	private final IndexColorModel planeColorModel;
	private int z = 0;
	private double yRel = 0;

	private boolean isStack = false;

	private final double relativeScaleAtZEnd = 0.7;

	public PlaneDisplay(IndexColorModel lut) {
		setBackground(Color.black);
		this.stackColorModels = prepareStackColorcode(ICamera.DEPTH, lut);
		this.planeColorModel = preparePlaneColorcode();
		this.setIgnoreRepaint(true);
		// this.setDoubleBuffered(true);
		addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				render();
			}

			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
	}

	public void setStackMode(boolean b) {
		isStack = b;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(WIDTH, HEIGHT);
	}

	public void display(byte[] fluorescence, byte[] transmission, double yRel, int z) {
		this.fluorescence = fluorescence;
		this.transmission = transmission;
		this.yRel = yRel;
		this.z = z;
		render();
	}

	public Image getFluorescenceImage() {
		IndexColorModel cm = isStack ? stackColorModels[z] : planeColorModel;
		BufferedImage bi = new BufferedImage(ICamera.WIDTH, ICamera.HEIGHT, BufferedImage.TYPE_BYTE_INDEXED, cm);
		byte[] array = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		System.arraycopy(fluorescence, 0, array, 0, array.length);
		return bi;
	}

	public Image getTransmissionImage() {
		BufferedImage bi = new BufferedImage(ICamera.WIDTH, ICamera.HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
		byte[] array = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		System.arraycopy(transmission, 0, array, 0, array.length);
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

		if(!isStack) {
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

		if(!isStack)
			drawCoordSysInt(renderedGraphics, scale);


		double zScale = 1 + (relativeScaleAtZEnd - 1) * z / ICamera.DEPTH;

		int imageWidth  = 2 * (int)Math.round(ICamera.WIDTH  * scale * zScale / 2.0);
		int imageHeight = 2 * (int)Math.round(ICamera.HEIGHT * scale * zScale / 2.0);

		int xOffs = (int)Math.round((w - imageWidth) / 2.0);
		int yOffs = (int)Math.round((h - imageHeight) / 2.0);

		if(!isStack && transmission != null)
			renderedGraphics.drawImage(getTransmissionImage(), xOffs, yOffs, imageWidth, imageHeight, null);

		if(fluorescence != null)
			renderedGraphics.drawImage(getFluorescenceImage(), xOffs, yOffs, imageWidth, imageHeight, null);

		g.drawImage(renderedImage, 0, 0, null);

		g.setColor(Color.red);
		g2d.setStroke(new BasicStroke(1.0f));
		g.drawRect(xOffs, yOffs, imageWidth - 1, imageHeight - 1);

		// draw the y-indicator
		imageWidth  = 2 * (int)Math.round(ICamera.WIDTH  * scale / 2.0);
		imageHeight = 2 * (int)Math.round(ICamera.HEIGHT * scale / 2.0);

		xOffs = (int)Math.round((w + imageWidth) / 2.0) + 20;
		yOffs = (int)Math.round((h - imageHeight) / 2.0);

		g.setColor(Color.LIGHT_GRAY);
		int cw = w - xOffs - 20;
		if(cw > 20)
			cw = 20;

		double factor = (double) cw / ICamera.WIDTH;
		int ch = (int)Math.round(factor * ICamera.HEIGHT);
		int fullh = ((int)(Math.abs(Preferences.getStackYEnd() - Preferences.getStackYStart()) // motor range
				/ Preferences.getPixelWidth() // in camera pixels
				/ ICamera.HEIGHT              // in multiples of camera heights
				* ch))                        // convert to drawing coords
				+ ch;                         // additional camera height

		if(fullh > imageHeight / 2) {
			double f = imageHeight / 2.0 / fullh;
			fullh = (int)Math.round(fullh * f);
			ch = (int)Math.round(ch * f);
			cw = (int)Math.round(cw * f);
		}
		g.fillRect(xOffs, yOffs + fullh - ch, cw, ch);

		int cy = (int)Math.round(yRel * (fullh - ch));
		g.drawRect(xOffs, yOffs + fullh - ch - cy, cw, fullh);
	}

	private static IndexColorModel[] prepareStackColorcode(int nlayers, IndexColorModel lut) {
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

	private static IndexColorModel preparePlaneColorcode() {
		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		byte[] a = new byte[256];

		for (int j = 0; j < 256; j++) {
			r[j] = 0;
			g[j] = (byte)j;
			b[j] = 0;
			a[j] = (byte)j;
			// newA[j] = (byte) Math.round(255 * Math.pow(j / 255.0, 2));
		}

		IndexColorModel cm = new IndexColorModel(8, 256, r, g, b, a);
		return cm;
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
		disp.isStack = false;
		for(int z = 0; z < 400; z++) {
			long start = System.currentTimeMillis();
			disp.display(data[z], null, 0, z);
			long end = System.currentTimeMillis();
			System.out.println(z + ": " + (end - start));
			// Thread.sleep(10);
		}
		disp.display(null, null, 0, 399);

		disp.isStack = true;
		for(int z = 399; z >= 0; z--) {
			disp.display(data[z], null, 0, z);
			// Thread.sleep(10);
		}
	}
}
