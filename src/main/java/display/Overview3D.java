package display;

import ij.ImagePlus;
import ij.io.Opener;

import java.awt.image.BufferedImage;
import java.io.InputStream;

public class Overview3D {

	private static final int yFrames = 60;
	private static final int zFrames = 10;

	private final BufferedImage[][] images;

	private final int width, height;

	public Overview3D() {
		ImagePlus imp = null;
		InputStream is = getClass().getResourceAsStream("/eduSPIM_overview_3D.tif");
		if(is != null) {
			Opener opener = new Opener();
			imp = opener.openTiff(is, "3D overview");
		}
		if(imp == null)
			throw new RuntimeException("Cannot load 3D overview");

		width = imp.getWidth();
		height = imp.getHeight();

		images = new BufferedImage[yFrames][zFrames];

		for(int y = 0, i = 0; y < yFrames; y++) {
			for(int z = 0; z < zFrames; z++, i++) {
				images[y][z] = imp.getStack().getProcessor(i + 1).getBufferedImage();
			}
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public BufferedImage get(int y, int z) {
		if(y < 0) y = 0;
		if(z < 0) z = 0;
		if(y >= yFrames) y = yFrames - 1;
		if(z >= zFrames) z = zFrames - 1;
		return images[y][z];
	}

	public BufferedImage get(double yRel, double zRel) {
		int y = (int)Math.round(yRel * yFrames);
		int z = (int)Math.round(zRel * zFrames);
		return get(y, z);
	}
}
