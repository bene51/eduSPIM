package main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import stage.IMotor;

public class Preferences {

	public static final String PROPERTY_FILE =
		System.getProperty("user.home") + File.separator + ".EduSPIM.props";

	public static final String STACK_Z_START  = "stack_z_start";
	public static final String STACK_Z_END    = "stack_z_end";
	public static final String STACK_Y_START  = "stack_y_start";
	public static final String STACK_Y_END    = "stack_y_end";
	public static final String MIRROR_Z1      = "mirror_z1";
	public static final String MIRROR_M1      = "mirror_m1";
	public static final String MIRROR_Z2      = "mirror_z2";
	public static final String MIRROR_M2      = "mirror_m2";
	public static final String MIRROR_COEFF_M = "mirror_coeff_m";
	public static final String MIRROR_COEFF_T = "mirror_coeff_t";
	public static final String PIXEL_WIDTH    = "pixel_width";
	public static final String SNAPSHOTS_DIR  = "snapshots_dir";
	public static final String LOGS_DIR       = "logs_dir";
	public static final String SNAPSHOTS_LINK = "snapshots_link";
	public static final String LOGS_LINK      = "logs_link";
	public static final String MAIL_TO        = "mail_to";
	public static final String MAIL_CC        = "mail_cc";

	private static final double DEFAULT_STACK_ZSTART   = IMotor.POS_MIN_Z;
	private static final double DEFAULT_STACK_ZEND     = IMotor.POS_MAX_Z;
	private static final double DEFAULT_STACK_YSTART   = IMotor.POS_MIN_Y;
	private static final double DEFAULT_STACK_YEND     = IMotor.POS_MAX_Y;
	private static final double DEFAULT_MIRROR_Z1      = IMotor.POS_MIN_Z;
	private static final double DEFAULT_MIRROR_M1      = 0;
	private static final double DEFAULT_MIRROR_Z2      = IMotor.POS_MAX_Z;
	private static final double DEFAULT_MIRROR_M2      = 0;
	private static final double DEFAULT_MIRROR_COEFF_M = 0;
	private static final double DEFAULT_MIRROR_COEFF_T = 0;
	private static final double DEFAULT_PIXEL_WIDHT    = 5.3 *  // pixel width on sensor
	                                                     0.1 *  // magnification
	                                                     0.001; // convert to mm
	private static final String DEFAULT_SNAPSHOTS_DIR  = System.getProperty("user.home") +
															File.separator + "Dropbox" +
															File.separator + "EduSPIM" +
															File.separator + "Snapshots";
	private static final String DEFAULT_LOGS_DIR       = System.getProperty("user.home") +
															File.separator + "Dropbox" +
															File.separator + "EduSPIM" +
															File.separator + "Logs";

	private static final String DEFAULT_LOGS_LINK      = "https://www.dropbox.com/sh/bccsscts2gc8i6r/AADQb3myigWu1JoZDVEyANWwa?dl=0";
	private static final String DEFAULT_SNAPSHOTS_LINK = "https://www.dropbox.com/sh/ooiudayauje1apw/AABzeD851uTbBJb0j-81XBCOa?dl=0";
	private static final String DEFAULT_MAIL_TO        = "bene.schmid@gmail.com";
	private static final String DEFAULT_MAIL_CC        = ""; // TODO labhuisken@mpi-cbg.de


	private static Preferences instance;

	private final Properties properties;
	private final File propertiesFile;

	private double stackZStart, stackZEnd, stackYStart, stackYEnd;
	private double mirrorZ1, mirrorM1, mirrorZ2, mirrorM2, mirrorCoeffM, mirrorCoeffT;
	private double  pixelWidth;
	private String snapshotsdir, logsdir;
	private String logslink, snapshotslink;
	private String mailto, mailcc;

	public static double getStackZStart() {
		return getInstance().stackZStart;
	}

	public static double getStackZEnd() {
		return getInstance().stackZEnd;
	}

	public static double getStackYStart() {
		return getInstance().stackYStart;
	}

	public static double getStackYEnd() {
		return getInstance().stackYEnd;
	}

	public static double getMirrorZ1() {
		return getInstance().mirrorZ1;
	}

	public static double getMirrorM1() {
		return getInstance().mirrorM1;
	}

	public static double getMirrorZ2() {
		return getInstance().mirrorZ2;
	}

	public static double getMirrorM2() {
		return getInstance().mirrorM2;
	}

	public static double getMirrorCoefficientM() {
		return getInstance().mirrorCoeffM;
	}

	public static double getMirrorCoefficientT() {
		return getInstance().mirrorCoeffT;
	}

	public static double getPixelWidth() {
		return getInstance().pixelWidth;
	}

	public static String getSnapshotsDir() {
		return getInstance().snapshotsdir;
	}

	public static String getLogsDir() {
		return getInstance().logsdir;
	}

	public static String getSnapshotsLink() {
		return getInstance().snapshotslink;
	}

	public static String getLogsLink() {
		return getInstance().logslink;
	}

	public static String getMailto() {
		return getInstance().mailto;
	}

	public static String getMailcc() {
		return getInstance().mailcc;
	}

	public static void setStackZStart(double stackZStart) {
		getInstance().stackZStart = stackZStart;
		set(STACK_Z_START, stackZStart);
	}

	public static void setStackZEnd(double stackZEnd) {
		getInstance().stackZEnd = stackZEnd;
		set(STACK_Z_END, stackZEnd);
	}

	public static void setStackYStart(double stackYStart) {
		getInstance().stackYStart = stackYStart;
		set(STACK_Y_START, stackYStart);
	}

	public static void setStackYEnd(double stackYEnd) {
		getInstance().stackYEnd = stackYEnd;
		set(STACK_Y_END, stackYEnd);
	}

	public static void setMirrorZ1(double mirrorZ1) {
		getInstance().mirrorZ1 = mirrorZ1;
		set(MIRROR_Z1, mirrorZ1);
	}

	public static void setMirrorM1(double mirrorM1) {
		getInstance().mirrorM1 = mirrorM1;
		set(MIRROR_M1, mirrorM1);
	}

	public static void setMirrorZ2(double mirrorZ2) {
		getInstance().mirrorZ2 = mirrorZ2;
		set(MIRROR_Z2, mirrorZ2);
	}

	public static void setMirrorM2(double mirrorM2) {
		getInstance().mirrorM2 = mirrorM2;
		set(MIRROR_M2, mirrorM2);
	}

	public static void setMirrorCoefficientM(double m) {
		getInstance().mirrorCoeffM = m;
		set(MIRROR_COEFF_M, m);
	}

	public static void setMirrorCoefficientT(double t) {
		getInstance().mirrorCoeffT = t;
		set(MIRROR_COEFF_T, t);
	}

	private Preferences() {
		properties = new Properties();
		properties.put(STACK_Z_START,  Double.toString(DEFAULT_STACK_ZSTART));
		properties.put(STACK_Z_END,    Double.toString(DEFAULT_STACK_ZEND));
		properties.put(STACK_Y_START,  Double.toString(DEFAULT_STACK_YSTART));
		properties.put(STACK_Y_END,    Double.toString(DEFAULT_STACK_YEND));
		properties.put(MIRROR_Z1,      Double.toString(DEFAULT_MIRROR_Z1));
		properties.put(MIRROR_M1,      Double.toString(DEFAULT_MIRROR_M1));
		properties.put(MIRROR_Z2,      Double.toString(DEFAULT_MIRROR_Z2));
		properties.put(MIRROR_M2,      Double.toString(DEFAULT_MIRROR_M2));
		properties.put(MIRROR_COEFF_M, Double.toString(DEFAULT_MIRROR_COEFF_M));
		properties.put(MIRROR_COEFF_T, Double.toString(DEFAULT_MIRROR_COEFF_T));
		properties.put(PIXEL_WIDTH,    Double.toString(DEFAULT_PIXEL_WIDHT));
		properties.put(SNAPSHOTS_DIR,  DEFAULT_SNAPSHOTS_DIR);
		properties.put(LOGS_DIR,       DEFAULT_LOGS_DIR);
		properties.put(SNAPSHOTS_LINK, DEFAULT_SNAPSHOTS_LINK);
		properties.put(LOGS_LINK,      DEFAULT_LOGS_LINK);
		properties.put(MAIL_TO,        DEFAULT_MAIL_TO);
		properties.put(MAIL_CC,        DEFAULT_MAIL_CC);

		propertiesFile = new File(PROPERTY_FILE);

		FileReader reader = null;
		try {
			reader = new FileReader(propertiesFile);
			properties.load(reader);
		} catch(Exception e) {
			ExceptionHandler.handleException("Error loading properties file", e);
		} finally {
			try {
				if(reader != null)
					reader.close();
			} catch(IOException e) {
				ExceptionHandler.handleException("Error closing properties file", e);
			}
		}
		stackZStart   = Double.parseDouble(properties.getProperty(STACK_Z_START,  Double.toString(DEFAULT_STACK_ZSTART)));
		stackZEnd     = Double.parseDouble(properties.getProperty(STACK_Z_END,    Double.toString(DEFAULT_STACK_ZEND)));
		stackYStart   = Double.parseDouble(properties.getProperty(STACK_Y_START,  Double.toString(DEFAULT_STACK_YSTART)));
		stackYEnd     = Double.parseDouble(properties.getProperty(STACK_Y_END,    Double.toString(DEFAULT_STACK_YEND)));
		mirrorZ1      = Double.parseDouble(properties.getProperty(MIRROR_Z1,      Double.toString(DEFAULT_MIRROR_Z1)));
		mirrorM1      = Double.parseDouble(properties.getProperty(MIRROR_M1,      Double.toString(DEFAULT_MIRROR_M1)));
		mirrorZ2      = Double.parseDouble(properties.getProperty(MIRROR_Z2,      Double.toString(DEFAULT_MIRROR_Z2)));
		mirrorM2      = Double.parseDouble(properties.getProperty(MIRROR_M2,      Double.toString(DEFAULT_MIRROR_M2)));
		mirrorCoeffM  = Double.parseDouble(properties.getProperty(MIRROR_COEFF_M, Double.toString(DEFAULT_MIRROR_COEFF_M)));
		mirrorCoeffT  = Double.parseDouble(properties.getProperty(MIRROR_COEFF_T, Double.toString(DEFAULT_MIRROR_COEFF_T)));
		pixelWidth    = Double.parseDouble(properties.getProperty(PIXEL_WIDTH,    Double.toString(DEFAULT_PIXEL_WIDHT)));
		snapshotsdir  = properties.getProperty(SNAPSHOTS_DIR, DEFAULT_SNAPSHOTS_DIR);
		logsdir       = properties.getProperty(LOGS_DIR, DEFAULT_LOGS_DIR);
		snapshotslink = properties.getProperty(SNAPSHOTS_LINK, DEFAULT_SNAPSHOTS_LINK);
		logslink      = properties.getProperty(LOGS_LINK, DEFAULT_LOGS_LINK);
		mailto        = properties.getProperty(MAIL_TO, DEFAULT_MAIL_TO);
		mailcc        = properties.getProperty(MAIL_CC, DEFAULT_MAIL_TO);
	}

	public static HashMap<String, String> backup() {
		HashMap<String, String> backup = new HashMap<String, String>();
		Preferences p = getInstance();
		Enumeration<?> e = p.properties.propertyNames();
		while(e.hasMoreElements()) {
			String key = (String)e.nextElement();
			backup.put(key, p.properties.getProperty(key));
		}
		return backup;
	}

	public static void restore(HashMap<String, String> backup) {
		Preferences p = getInstance();
		p.stackZStart   = Double.parseDouble(backup.get(STACK_Z_START));
		p.stackZEnd     = Double.parseDouble(backup.get(STACK_Z_END));
		p.stackYStart   = Double.parseDouble(backup.get(STACK_Y_START));
		p.stackYEnd     = Double.parseDouble(backup.get(STACK_Y_END));
		p.mirrorZ1      = Double.parseDouble(backup.get(MIRROR_Z1));
		p.mirrorM1      = Double.parseDouble(backup.get(MIRROR_M1));
		p.mirrorZ2      = Double.parseDouble(backup.get(MIRROR_Z2));
		p.mirrorM2      = Double.parseDouble(backup.get(MIRROR_M2));
		p.mirrorCoeffM  = Double.parseDouble(backup.get(MIRROR_COEFF_M));
		p.mirrorCoeffT  = Double.parseDouble(backup.get(MIRROR_COEFF_T));
		p.pixelWidth    = Double.parseDouble(backup.get(PIXEL_WIDTH));
		p.snapshotsdir  = backup.get(SNAPSHOTS_DIR);
		p.logsdir       = backup.get(LOGS_DIR);
		p.snapshotslink = backup.get(SNAPSHOTS_LINK);
		p.logslink      = backup.get(LOGS_LINK);
		p.mailto        = backup.get(MAIL_TO);
		p.mailcc        = backup.get(MAIL_CC);
		Preferences.setAll(backup);
	}

	private static Preferences getInstance() {
		if(instance == null)
			instance = new Preferences();
		return instance;
	}

	private static synchronized String get(String key) {
		return getInstance().properties.getProperty(key);
	}

	private static int getInt(String key) {
		return Integer.parseInt(get(key));
	}

	private static double getDouble(String key) {
		return Double.parseDouble(get(key));
	}

	private static void setAll(HashMap<String, String> table) {
		for(String key : table.keySet())
			getInstance().properties.setProperty(key, table.get(key));
		save(getInstance().propertiesFile);
	}

	private static void set(String key, Object value) {
		getInstance().properties.setProperty(key, value.toString());
		save(getInstance().propertiesFile);
	}

	private static void save(File file) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(file);
			doWrite(out);
		} catch(Exception e) {
			ExceptionHandler.handleException("Error saving properties file", e);
		} finally {
			try {
				if(out != null)
					out.close();
			} catch(Exception e) {
				ExceptionHandler.handleException("Error closing properties file", e);
			}
		}
	}

	private static void doWrite(PrintWriter out) {
		Preferences p = getInstance();
		out.println("# Properties for EduSPIM");
		out.println();
		out.println("# Volume limits:");
		out.println(STACK_Y_START + "=" + p.stackYStart);
		out.println(STACK_Z_START + "=" + p.stackZStart);
		out.println(STACK_Y_END   + "=" + p.stackYEnd);
		out.println(STACK_Z_END   + "=" + p.stackZEnd);
		out.println();
		out.println("# Mirror calibration");
		out.println(MIRROR_Z1 + "=" + p.mirrorZ1);
		out.println(MIRROR_M1 + "=" + p.mirrorM1);
		out.println(MIRROR_Z2 + "=" + p.mirrorZ2);
		out.println(MIRROR_M2 + "=" + p.mirrorM2);
		out.println("# Resulting offset and slope");
		out.println(MIRROR_COEFF_T + "=" + p.mirrorCoeffT);
		out.println(MIRROR_COEFF_M + "=" + p.mirrorCoeffM);
		out.println();
		out.println("# Physical width of a pixel, in mm");
		out.println(PIXEL_WIDTH + "=" + p.pixelWidth);
		out.println();
		out.println("# Folder where snapshots are written to");
		out.println(SNAPSHOTS_DIR + "=" + escape(p.snapshotsdir));
		out.println();
		out.println("# Folder where logs are written to");
		out.println(LOGS_DIR + "=" + escape(p.logsdir));
		out.println();
		out.println("# Links that point a browser to shared logs/snapshots directories,");
		out.println("# included in some emails");
		out.println(SNAPSHOTS_LINK + "=" + escape(p.snapshotslink));
		out.println(LOGS_LINK + "=" + escape(p.logslink));
		out.println();
		out.println("# Email addresses");
		out.println(MAIL_TO + "=" + escape(p.mailto));
		out.println(MAIL_CC + "=" + escape(p.mailcc));
	}

	private static String escape(String s) {
		while(s.endsWith("\\"))
			s = s.substring(0, s.length() - 1);
		return s.replaceAll("\\\\", "\\\\\\\\")
				.replaceAll(":", "\\\\:")
				.replaceAll("=", "\\\\=");
	}
}

