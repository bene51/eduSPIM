package main;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import mpicbg.models.InvertibleBoundable;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import mpicbg.stitching.CollectionStitchingImgLib;
import mpicbg.stitching.ImageCollectionElement;
import mpicbg.stitching.ImagePlusTimePoint;
import mpicbg.stitching.StitchingParameters;
import mpicbg.stitching.TextFileAccess;
import mpicbg.stitching.fusion.Fusion;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class Stitching {

	public static ImagePlus stitch(String directory, int gridSizeX, int gridSizeY) {

		// the general stitching parameters
		final StitchingParameters params = new StitchingParameters();

		double overlapX = 0.15, overlapY = 0.15;
		int startI = 1;

		final String filenames = "tile{i}.tif";
		String outputFile = "TileConfiguration.txt";

		params.fusionMethod = 0; // linear blending
		params.regThreshold = 0.3;
		params.relativeThreshold = 2.5;
		params.absoluteThreshold = 3.5;

		params.addTilesAsRois = false;
		params.computeOverlap = true;

		params.invertX = false;
		params.invertY = false;
		params.ignoreZStage = false;

		params.subpixelAccuracy = false;
		params.downSample = false;
		params.displayFusion = false;
		params.virtual = false;
		params.cpuMemChoice = 0; // save memory
		params.outputVariant = 0; // fuse and display (1: write to disk)
		params.dimensionality = 3;

		// we need to set this
		params.channel1 = 0;
		params.channel2 = 0;
		params.timeSelect = 0;
		params.checkPeaks = 5;

		// for reading in writing the tileconfiguration file
		directory = directory.replace('\\', '/');
		directory = directory.trim();
		if (directory.length() > 0 && !directory.endsWith("/"))
			directory = directory + "/";

		params.outputDirectory = directory;

		// get all imagecollectionelements
		final ArrayList<ImageCollectionElement> elements = getGridLayout(
				gridSizeX, gridSizeY, overlapX, overlapY, directory, filenames,
				startI, params.virtual);

		// open all images (if not done already by grid parsing) and test them,
		// collect information
		int numTimePoints = -1;

		for (final ImageCollectionElement element : elements) {
			final ImagePlus imp = element.open(params.virtual);
			if (imp == null)
				return null;

			numTimePoints = imp.getNFrames();
		}

		// write the initial tileconfiguration
		writeTileConfiguration(new File(directory, outputFile), elements);

		// call the final stitching
		final ArrayList<ImagePlusTimePoint> optimized = CollectionStitchingImgLib
				.stitchCollection(elements, params);

		if (optimized == null)
			return null;

		// write the file tileconfiguration
		// NOTE: outputFile should never be null anyway!
		if (params.computeOverlap && outputFile != null) {
			if (outputFile.endsWith(".txt"))
				outputFile = outputFile.substring(0, outputFile.length() - 4)
						+ ".registered.txt";
			else
				outputFile = outputFile + ".registered.txt";

			writeRegisteredTileConfiguration(new File(directory, outputFile),
					elements);
		}

		IJ.showStatus("Fusing stitched image...");

		// first prepare the models and get the targettype
		final ArrayList<InvertibleBoundable> models = new ArrayList<InvertibleBoundable>();
		final ArrayList<ImagePlus> images = new ArrayList<ImagePlus>();

		for (final ImagePlusTimePoint imt : optimized) {
			final ImagePlus imp = imt.getImagePlus();
			images.add(imp);
		}

		for (int f = 1; f <= numTimePoints; ++f)
			for (final ImagePlusTimePoint imt : optimized)
				models.add((InvertibleBoundable) imt.getModel());

		boolean noOverlap = false;

		ImagePlus imp = Fusion.fuse(new UnsignedByteType(), images, models,
				params.dimensionality, params.subpixelAccuracy,
				params.fusionMethod, params.outputDirectory, noOverlap, false,
				params.displayFusion);

		if (imp != null)
			imp.setTitle("Fused");

		// close all images
		for (final ImageCollectionElement element : elements)
			element.close();

		return imp;
	}

	public static void postProcess(ImagePlus stitched) {
		IJ.showStatus("Post-processing stitched image");
		int d = stitched.getStackSize();
		for(int z = 0; z < d; z++) {
			ImageProcessor ip = stitched.getStack().getProcessor(z + 1);
			postProcess(ip);
			IJ.showProgress(z + 1, d);
		}
	}

	private static void postProcess(ImageProcessor ip) {
		int w = ip.getWidth();
		int h = ip.getHeight();

		for(int y = 0; y < h; y++) {
			// start of line:
			int x0 = 0;
			for(x0 = 0; x0 < w; x0++)
				if(ip.getf(x0, y) != 0)
					break;

			if(x0 == w)
				continue;

			for(int x = x0 - 1; x >= 0; x--)
				ip.setf(x, y, ip.getf(x0 + x0 - x, y));

			// end of line:
			for(x0 = w - 1; x0 >= 0; x0--)
				if(ip.getf(x0, y) != 0)
					break;

			for(int x = x0 + 1; x < w; x++)
				ip.setf(x, y, ip.getf(x0 - (x - x0), y));
		}
	}

	private static ArrayList<ImageCollectionElement> getGridLayout(
			final int gridSizeX, final int gridSizeY, final double overlapX,
			final double overlapY, final String directory,
			final String filenames, final int startI, final boolean virtual) {
		// define the parsing of filenames
		// find how to parse
		String replaceI = "{";
		int numIValues = 0;

		int i1 = filenames.indexOf("{i");
		int i2 = filenames.indexOf("i}");
		if (i1 >= 0 && i2 > 0) {
			numIValues = i2 - i1;
			for (int i = 0; i < numIValues; i++)
				replaceI += "i";
			replaceI += "}";
		} else {
			replaceI = "\\\\\\\\";
		}

		// determine the layout
		final ImageCollectionElement[][] gridLayout = new ImageCollectionElement[gridSizeX][gridSizeY];

		// all snakes, row, columns, whatever
		// the current position[x, y]
		final int[] position = new int[2];

		// we have gridSizeX * gridSizeY tiles
		for (int i = 0; i < gridSizeX * gridSizeY; ++i) {
			// get the vector where to move
			getPosition(position, i, gridSizeX);

			// get the filename
			final String file = filenames.replace(replaceI,
					getLeadingZeros(numIValues, i + startI));
			gridLayout[position[0]][position[1]] = new ImageCollectionElement(
					new File(directory, file), i);
		}

		// based on the minimum size we will compute the initial arrangement
		int minWidth = Integer.MAX_VALUE;
		int minHeight = Integer.MAX_VALUE;
		int minDepth = Integer.MAX_VALUE;

		// open all images and test them, collect information
		for (int y = 0; y < gridSizeY; ++y) {
			for (int x = 0; x < gridSizeX; ++x) {
				final ImagePlus imp = gridLayout[x][y].open(virtual);

				if (imp == null)
					return null;

				if (imp.getWidth() < minWidth)
					minWidth = imp.getWidth();

				if (imp.getHeight() < minHeight)
					minHeight = imp.getHeight();

				if (imp.getNSlices() < minDepth)
					minDepth = imp.getNSlices();
			}
		}

		final int dimensionality = 3;

		// now get the approximate coordinates for each element
		// that is easiest done incrementally
		int xoffset = 0;

		// an ArrayList containing all the ImageCollectionElements
		final ArrayList<ImageCollectionElement> elements = new ArrayList<ImageCollectionElement>();

		for (int y = 0; y < gridSizeY; y++) {
			int yoffset = y * (int) (minHeight * (1 - overlapY));
			for (int x = 0; x < gridSizeX; x++) {
				final ImageCollectionElement element = gridLayout[x][y];
				xoffset = x * (int) (minWidth * (1 - overlapX));

				element.setDimensionality(dimensionality);
				element.setModel(new TranslationModel3D());
				element.setOffset(new float[] { xoffset, yoffset, 0 });

				elements.add(element);
			}
		}
		return elements;
	}

	private static void writeTileConfiguration(final File file,
			final ArrayList<ImageCollectionElement> elements) {
		// write the initial tileconfiguration
		final PrintWriter out = TextFileAccess.openFileWrite(file);
		final int dimensionality = elements.get(0).getDimensionality();

		out.println("# Define the number of dimensions we are working on");
		out.println("dim = " + dimensionality);
		out.println("");
		out.println("# Define the image coordinates");

		for (final ImageCollectionElement element : elements) {
			if (dimensionality == 3)
				out.println(element.getFile().getName() + "; ; ("
						+ element.getOffset(0) + ", " + element.getOffset(1)
						+ ", " + element.getOffset(2) + ")");
			else
				out.println(element.getFile().getName() + "; ; ("
						+ element.getOffset(0) + ", " + element.getOffset(1)
						+ ")");
		}

		out.close();
	}

	private static void writeRegisteredTileConfiguration(final File file,
			final ArrayList<ImageCollectionElement> elements) {
		// write the tileconfiguration using the translation model
		final PrintWriter out = TextFileAccess.openFileWrite(file);
		final int dimensionality = elements.get(0).getDimensionality();

		out.println("# Define the number of dimensions we are working on");
		out.println("dim = " + dimensionality);
		out.println("");
		out.println("# Define the image coordinates");

		for (final ImageCollectionElement element : elements) {
			if (dimensionality == 3) {
				final TranslationModel3D m = (TranslationModel3D) element
						.getModel();
				out.println(element.getFile().getName() + "; ; ("
						+ m.getTranslation()[0] + ", " + m.getTranslation()[1]
						+ ", " + m.getTranslation()[2] + ")");
			} else {
				final TranslationModel2D m = (TranslationModel2D) element
						.getModel();
				final double[] tmp = new double[2];
				m.applyInPlace(tmp);

				out.println(element.getFile().getName() + "; ; (" + tmp[0]
						+ ", " + tmp[1] + ")");
			}
		}

		out.close();
	}

	private static void getPosition(final int[] currentPosition, final int i,
			final int sizeX) {
		if (i == 0) {
			currentPosition[0] = 0;
			currentPosition[1] = 0;
		} else { // a move is required
			if (currentPosition[0] < sizeX - 1)
				++currentPosition[0];
			else {
				// we have to change rows
				++currentPosition[1];

				// row-by-row going right, so only set position to 0
				currentPosition[0] = 0;
			}
		}
	}

	private static String getLeadingZeros(final int zeros, final int number) {
		String output = "" + number;

		while (output.length() < zeros)
			output = "0" + output;

		return output;
	}
}
