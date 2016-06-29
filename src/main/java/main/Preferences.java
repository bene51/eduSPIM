package main;

import ij.gui.GenericDialog;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import stage.IMotor;

public class Preferences {

	public static final String PROPERTY_FILE_DIR = System.getProperty("user.home") + File.separator + ".eduSPIM" + File.separator;
	public static final String PROPERTY_FILE = PROPERTY_FILE_DIR + "EduSPIM.props";

	public static final String STACK_Z_START        = "stack_z_start";
	public static final String STACK_Z_END          = "stack_z_end";
	public static final String STACK_Y_START        = "stack_y_start";
	public static final String STACK_Y_END          = "stack_y_end";
	public static final String MIRROR_Z1            = "mirror_z1";
	public static final String MIRROR_M1            = "mirror_m1";
	public static final String MIRROR_Z2            = "mirror_z2";
	public static final String MIRROR_M2            = "mirror_m2";
	public static final String MIRROR_COEFF_M       = "mirror_coeff_m";
	public static final String MIRROR_COEFF_T       = "mirror_coeff_t";
	public static final String LASER_POWER          = "laser_power";
	public static final String CAMERA_F_EXPOSURE    = "camera_fluorescence_exposure";
	public static final String CAMERA_F_FRAMERATE   = "camera_fluorescence_framerate";
	public static final String CAMERA_F_GAIN        = "camera_fluorescence_gain";
	public static final String CAMERA_T_EXPOSURE    = "camera_transmission_exposure";
	public static final String CAMERA_T_FRAMERATE   = "camera_transmission_framerate";
	public static final String CAMERA_T_GAIN        = "camera_transmission_gain";
	public static final String PIXEL_WIDTH          = "pixel_width";
	public static final String STACKS_DIR           = "stacks_dir";
	public static final String LOGS_DIR             = "logs_dir";
	public static final String PROPERTIES_DIR       = "properties_dir";
	public static final String STATISTICS_PATH      = "statistics_path";
	public static final String SNAPSHOT_PATH        = "snapshot_path";
	public static final String STACKS_LINK          = "stacks_link";
	public static final String LOGS_LINK            = "logs_link";
	public static final String STATISTICS_LINK      = "statistics_link";
	public static final String MAIL_TO              = "mail_to";
	public static final String MAIL_CC              = "mail_cc";
	public static final String MAIL_SMTP_USER       = "smtp_username";
	public static final String MAIL_SMTP_PASSWORD   = "smtp_password";
	public static final String FAIL_WITHOUT_ARDUINO = "fail_without_arduino";
	public static final String SIMULATING           = "simulating";

	private static final double DEFAULT_STACK_ZSTART          = IMotor.POS_MIN_Z;
	private static final double DEFAULT_STACK_ZEND            = IMotor.POS_MIN_Z + 1;
	private static final double DEFAULT_STACK_YSTART          = IMotor.POS_MIN_Y;
	private static final double DEFAULT_STACK_YEND            = IMotor.POS_MIN_Y + 5;
	private static final double DEFAULT_MIRROR_Z1             = IMotor.POS_MIN_Z;
	private static final double DEFAULT_MIRROR_M1             = 1;
	private static final double DEFAULT_MIRROR_Z2             = IMotor.POS_MAX_Z;
	private static final double DEFAULT_MIRROR_M2             = 1;
	private static final double DEFAULT_MIRROR_COEFF_M        = 0;
	private static final double DEFAULT_MIRROR_COEFF_T        = 0;
	private static final double DEFAULT_LASER_POWER           = 50;
	private static final double DEFAULT_CAMERA_F_EXPOSURE     = 33.3;
	private static final double DEFAULT_CAMERA_F_FRAMERATE    = 30;
	private static final int    DEFAULT_CAMERA_F_GAIN         = 1;
	private static final double DEFAULT_CAMERA_T_EXPOSURE     = 33.3;
	private static final double DEFAULT_CAMERA_T_FRAMERATE    = 30;
	private static final int    DEFAULT_CAMERA_T_GAIN         = 1;
	private static final double DEFAULT_PIXEL_WIDHT           = 5.3 *  // pixel width on sensor
	                                                            0.1 *  // magnification
	                                                            0.001; // convert to mm
	private static final String DEFAULT_STACKS_DIR            = PROPERTY_FILE_DIR + "Stacks";
	private static final String DEFAULT_LOGS_DIR              = PROPERTY_FILE_DIR + "Logs";
	private static final String DEFAULT_PROPERTIES_DIR        = PROPERTY_FILE_DIR + "Settings";
	private static final String DEFAULT_STATISTICS_PATH       = PROPERTY_FILE_DIR + "statistics.csv";
	private static final String DEFAULT_SNAPSHOT_PATH         = PROPERTY_FILE_DIR + "snapshot.png";

	private static final String DEFAULT_LOGS_LINK             = "";
	private static final String DEFAULT_STACKS_LINK           = "";
	private static final String DEFAULT_STATISTICS_LINK       = "";
	private static final String DEFAULT_MAIL_TO               = "";
	private static final String DEFAULT_MAIL_CC               = "";
	private static final String DEFAULT_MAIL_SMTP_USER        = "";
	private static final String DEFAULT_MAIL_SMTP_PASSWORD    = "";
	private static final boolean DEFAULT_FAIL_WITHOUT_ARDUINO = false;
	private static final boolean DEFAULT_SIMULATING           = false;


	private static Preferences instance;

	public static void main(String[] args) {
		new Preferences();
	}

	private void initialConfiguration() {
		if(!new File(PROPERTY_FILE).exists()) {
			GenericDialog gd = new GenericDialog("Initial configuration");
			gd.addMessage(
					"Please enter some information in the following dialogs.\n" +
					"You can cancel at any time, in which case default values for all\n" +
					"parameters will be used. The software will still be usable, but\n" +
					"you won't get any email notifications.");
			gd.showDialog();
			if(gd.wasCanceled())
				return;

			gd = new GenericDialog("Initial configuration");
			gd.addMessage(
					"Please enter a (possibly shared) directory \n" +
					"where stack renderings are saved. Leave empty \n" +
					"to skip saving stack renderings");
			gd.addStringField("Stack_directory", DEFAULT_STACKS_DIR, 30);
			gd.addMessage(
					"Please enter a link under which the stack \n" +
					"directory can be accessed publically (e.g. a \n" +
					"publically shared Dropbox link) (optional)");
			gd.addStringField("Stacks_link", DEFAULT_STACKS_LINK, 30);
			gd.showDialog();
			if(gd.wasCanceled())
				return;
			String stacksdir = gd.getNextString();
			String stackslink = gd.getNextString();

			gd = new GenericDialog("Initial configuration");
			gd.addMessage(
					"Please enter a (possibly shared) directory \n" +
					"where log files are saved. Leave empty \n" +
					"to skip saving logs");
			gd.addStringField("Log_directory", DEFAULT_LOGS_DIR, 30);
			gd.addMessage(
					"Please enter a link under which the log \n" +
					"directory can be accessed publically (e.g. a \n" +
					"publically shared Dropbox link) (optional)");
			gd.addStringField("Log_link", DEFAULT_LOGS_LINK, 30);
			gd.showDialog();
			if(gd.wasCanceled())
				return;
			String logsdir = gd.getNextString();
			String logslink = gd.getNextString();

			gd = new GenericDialog("Initial configuration");
			gd.addMessage(
					"Please enter a (possibly shared) path to a statistics \n" +
					"file (csv). This file saves information about user button presses \n" +
					"and can be used for evaluating microscope usage, e.g. for the \n" +
					"website. Leave empty to not write a statistics file.");
			gd.addStringField("Statistics_path", DEFAULT_STATISTICS_PATH, 30);
			gd.addMessage(
					"Please enter a link under which the statistics \n" +
					"file can be accessed publically (e.g. a \n" +
					"publically shared Dropbox link) (optional)");
			gd.addStringField("Statistics_link", DEFAULT_STATISTICS_LINK, 30);
			gd.addMessage(
					"Please enter a (possibly shared) path (png file) where the \n" +
					"latest snapshot will be saved as an image. \n" +
					"Leave empty to not save snapshots.");
			gd.addStringField("Snapshot_path", DEFAULT_SNAPSHOT_PATH, 30);
			gd.showDialog();
			if(gd.wasCanceled())
				return;
			String statisticspath = gd.getNextString();
			String statisticslink = gd.getNextString();
			String snapshotpath = gd.getNextString();


			gd = new GenericDialog("Initial configuration");
			gd.addMessage(
					"Please enter a (possibly shared) directory for saving backups \n" +
					"of used settings. Leave empty to not backing up settings files.");
			gd.addStringField("Settings_folder", DEFAULT_PROPERTIES_DIR, 30);
			gd.addMessage(
					"Please enter a primary email address to which emails are sent \n" +
					"on startup, shut down or if an error occurs. Leave empty to \n" +
					"not send emails.");
			gd.addStringField("Primary_email", DEFAULT_MAIL_TO, 30);
			gd.addMessage(
					"Please enter an optional secondary email address. Emails are \n" +
					"CC'ed to this address.");
			gd.addStringField("Secondary_email", DEFAULT_MAIL_CC, 30);
			gd.addMessage(
					"Please enter a user name for the Google mail account that is \n" +
					"used for sending emails. Leave empty to not send emails.");
			gd.addStringField("SMTP_username", DEFAULT_MAIL_SMTP_USER, 30);
			gd.addMessage(
					"Please enter a password for the Google mail account that is \n" +
					"used for sending emails. Leave empty to not send emails. Attention: \n" +
					"This password is saved as plain text on your hard-drive");
			gd.addStringField("SMTP_password", DEFAULT_MAIL_SMTP_PASSWORD, 30);
			gd.addMessage(
					"Please specify whether the software should fail (show \n" +
					"an error screen) if no Arduino is connected to receive \n" +
					"button inputs. If you do not check the box, the software \n" +
					"will still be usable using the computer keyboard");
			gd.addCheckbox("Fail_without_Arduino", DEFAULT_FAIL_WITHOUT_ARDUINO);
			gd.addCheckbox("Simulate normal operation using pre-acquired data", DEFAULT_SIMULATING);
			gd.showDialog();
			if(gd.wasCanceled())
				return;
			String propertiesdir = gd.getNextString();
			String mailto = gd.getNextString();
			String mailcc = gd.getNextString();
			String mailuser = gd.getNextString();
			String mailpassword = gd.getNextString();
			boolean failWithoutArduino = gd.getNextBoolean();
			boolean simulating = gd.getNextBoolean();

			this.stacksdir = stacksdir;
			this.logsdir = logsdir;
			this.stackslink = stackslink;
			this.logslink = logslink;
			this.statisticspath = statisticspath;
			this.snapshotpath = snapshotpath;
			this.statisticslink = statisticslink;
			this.propertiesdir = propertiesdir;
			this.mailto = mailto;
			this.mailcc = mailcc;
			this.mailuser = mailuser;
			this.mailpassword = mailpassword;
			this.failWithoutArduino = failWithoutArduino;
			this.simulating = simulating;
		}
	}

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

	public static double getLaserPower() {
		return getInstance().laserpower;
	}

	public static double getTCameraFramerate() {
		return getInstance().tCameraFPS;
	}

	public static double getTCameraExposure() {
		return getInstance().tCameraExp;
	}

	public static int getTCameraGain() {
		return getInstance().tCameraGain;
	}

	public static double getFCameraFramerate() {
		return getInstance().fCameraFPS;
	}

	public static double getFCameraExposure() {
		return getInstance().fCameraExp;
	}

	public static int getFCameraGain() {
		return getInstance().fCameraGain;
	}

	public static double getPixelWidth() {
		return getInstance().pixelWidth;
	}

	public static String getStacksDir() {
		return getInstance().stacksdir;
	}

	public static String getLogsDir() {
		return getInstance().logsdir;
	}

	public static String getPropertiesDir() {
		return getInstance().propertiesdir;
	}

	public static String getStatisticsPath() {
		return getInstance().statisticspath;
	}

	public static String getSnapshotPath() {
		return getInstance().snapshotpath;
	}

	public static String getStacksLink() {
		return getInstance().stackslink;
	}

	public static String getLogsLink() {
		return getInstance().logslink;
	}

	public static String getStatisticsLink() {
		return getInstance().statisticslink;
	}

	public static String getMailto() {
		return getInstance().mailto;
	}

	public static String getMailcc() {
		return getInstance().mailcc;
	}

	public static String getSMTPUsername() {
		return getInstance().mailuser;
	}

	public static String getSMTPPassword() {
		return getInstance().mailpassword;
	}

	public static boolean getFailWithoutArduino() {
		return getInstance().failWithoutArduino;
	}

	public static boolean isSimulating() {
		return getInstance().simulating;
	}

	public static void setStackZStart(double stackZStart) {
		getInstance().stackZStart = stackZStart;
		getInstance().write();
	}

	public static void setStackZEnd(double stackZEnd) {
		getInstance().stackZEnd = stackZEnd;
		getInstance().write();
	}

	public static void setStackYStart(double stackYStart) {
		getInstance().stackYStart = stackYStart;
		getInstance().write();
	}

	public static void setStackYEnd(double stackYEnd) {
		getInstance().stackYEnd = stackYEnd;
		getInstance().write();
	}

	public static void setMirrorZ1(double mirrorZ1) {
		getInstance().mirrorZ1 = mirrorZ1;
		getInstance().write();
	}

	public static void setMirrorM1(double mirrorM1) {
		getInstance().mirrorM1 = mirrorM1;
		getInstance().write();
	}

	public static void setMirrorZ2(double mirrorZ2) {
		getInstance().mirrorZ2 = mirrorZ2;
		getInstance().write();
	}

	public static void setMirrorM2(double mirrorM2) {
		getInstance().mirrorM2 = mirrorM2;
		getInstance().write();
	}

	public static void setMirrorCoefficientM(double m) {
		getInstance().mirrorCoeffM = m;
		getInstance().write();
	}

	public static void setMirrorCoefficientT(double t) {
		getInstance().mirrorCoeffT = t;
		getInstance().write();
	}

	public static void setLaserPower(double p) {
		getInstance().laserpower = p;
		getInstance().write();
	}

	public static void setTCameraFramerate(double fps) {
		getInstance().tCameraFPS = fps;
		getInstance().write();
	}

	public static void setTCameraExposure(double t) {
		getInstance().tCameraExp = t;
		getInstance().write();
	}

	public static void setTCameraGain(int gain) {
		getInstance().tCameraGain = gain;
		getInstance().write();
	}

	public static void setFCameraFramerate(double fps) {
		getInstance().fCameraFPS = fps;
		getInstance().write();
	}

	public static void setFCameraExposure(double t) {
		getInstance().fCameraExp = t;
		getInstance().write();
	}

	public static void setFCameraGain(int gain) {
		getInstance().fCameraGain = gain;
		getInstance().write();
	}

	public static void setSimulating(boolean simulating) {
		getInstance().simulating = simulating;
		getInstance().write();
	}

	public static Properties backup() {
		return getInstance().toProperties();
	}

	public static void restoreAndSave(Properties properties) {
		Preferences p = getInstance();
		p.fromProperties(properties);
		p.write();
	}

	private static Preferences getInstance() {
		if(instance == null)
			instance = new Preferences();
		return instance;
	}

	public static void save(File file) {
		getInstance().write(file);
	}

	/********************************************************
	 * Here starts the non-static part.
	 ********************************************************/
	private final File propertiesFile;

	private double stackZStart, stackZEnd, stackYStart, stackYEnd;
	private double mirrorZ1, mirrorM1, mirrorZ2, mirrorM2, mirrorCoeffM, mirrorCoeffT;
	private double laserpower;
	private double tCameraFPS, tCameraExp, fCameraFPS, fCameraExp;
	private int tCameraGain, fCameraGain;
	private double  pixelWidth;
	private String stacksdir, logsdir, propertiesdir, statisticspath, snapshotpath;
	private String logslink, stackslink, statisticslink;
	private String mailto, mailcc, mailuser, mailpassword;
	private boolean failWithoutArduino, simulating;

	private Preferences() {
		Properties properties = new Properties();
		propertiesFile = new File(PROPERTY_FILE);

		FileReader reader = null;
		try {
			reader = new FileReader(propertiesFile);
			properties.load(reader);
		} catch(Exception e) {
			initialConfiguration();
			// ExceptionHandler.handleException("Error loading properties file", e);
		} finally {
			try {
				if(reader != null)
					reader.close();
			} catch(IOException e) {
				ExceptionHandler.handleException("Error closing properties file", e);
			}
		}
		// properties might not contain a value for every key. If a value is missing, the default will be used (see restore()).
		fromProperties(properties);
		write();
	}

	private Properties toProperties() {
		Properties properties = new Properties();
		properties.put(STACK_Z_START,        Double.toString(stackZStart));
		properties.put(STACK_Z_END,          Double.toString(stackZEnd));
		properties.put(STACK_Y_START,        Double.toString(stackYStart));
		properties.put(STACK_Y_END,          Double.toString(stackYEnd));
		properties.put(MIRROR_Z1,            Double.toString(mirrorZ1));
		properties.put(MIRROR_M1,            Double.toString(mirrorM1));
		properties.put(MIRROR_Z2,            Double.toString(mirrorZ2));
		properties.put(MIRROR_M2,            Double.toString(mirrorM2));
		properties.put(MIRROR_COEFF_M,       Double.toString(mirrorCoeffM));
		properties.put(MIRROR_COEFF_T,       Double.toString(mirrorCoeffT));
		properties.put(LASER_POWER,          Double.toString(laserpower));
		properties.put(CAMERA_T_FRAMERATE,   Double.toString(tCameraFPS));
		properties.put(CAMERA_T_EXPOSURE,    Double.toString(tCameraExp));
		properties.put(CAMERA_T_GAIN,        Integer.toString(tCameraGain));
		properties.put(CAMERA_F_FRAMERATE,   Double.toString(fCameraFPS));
		properties.put(CAMERA_F_EXPOSURE,    Double.toString(fCameraExp));
		properties.put(CAMERA_F_GAIN,        Integer.toString(fCameraGain));
		properties.put(PIXEL_WIDTH,          Double.toString(pixelWidth));
		properties.put(STACKS_DIR,           stacksdir);
		properties.put(LOGS_DIR,             logsdir);
		properties.put(PROPERTIES_DIR,       propertiesdir);
		properties.put(STATISTICS_PATH,      statisticspath);
		properties.put(SNAPSHOT_PATH,        snapshotpath);
		properties.put(STACKS_LINK,          stackslink);
		properties.put(LOGS_LINK,            logslink);
		properties.put(STATISTICS_LINK,      statisticslink);
		properties.put(MAIL_TO,              mailto);
		properties.put(MAIL_CC,              mailcc);
		properties.put(MAIL_SMTP_USER,       mailuser);
		properties.put(MAIL_SMTP_PASSWORD,   mailpassword);
		properties.put(FAIL_WITHOUT_ARDUINO, Boolean.toString(failWithoutArduino));
		properties.put(SIMULATING,           Boolean.toString(simulating));
		return properties;
	}

	private void fromProperties(Properties properties) {
		stackZStart   = Double.parseDouble(properties.getProperty(STACK_Z_START,      Double.toString(DEFAULT_STACK_ZSTART)));
		stackZEnd     = Double.parseDouble(properties.getProperty(STACK_Z_END,        Double.toString(DEFAULT_STACK_ZEND)));
		stackYStart   = Double.parseDouble(properties.getProperty(STACK_Y_START,      Double.toString(DEFAULT_STACK_YSTART)));
		stackYEnd     = Double.parseDouble(properties.getProperty(STACK_Y_END,        Double.toString(DEFAULT_STACK_YEND)));
		mirrorZ1      = Double.parseDouble(properties.getProperty(MIRROR_Z1,          Double.toString(DEFAULT_MIRROR_Z1)));
		mirrorM1      = Double.parseDouble(properties.getProperty(MIRROR_M1,          Double.toString(DEFAULT_MIRROR_M1)));
		mirrorZ2      = Double.parseDouble(properties.getProperty(MIRROR_Z2,          Double.toString(DEFAULT_MIRROR_Z2)));
		mirrorM2      = Double.parseDouble(properties.getProperty(MIRROR_M2,          Double.toString(DEFAULT_MIRROR_M2)));
		mirrorCoeffM  = Double.parseDouble(properties.getProperty(MIRROR_COEFF_M,     Double.toString(DEFAULT_MIRROR_COEFF_M)));
		mirrorCoeffT  = Double.parseDouble(properties.getProperty(MIRROR_COEFF_T,     Double.toString(DEFAULT_MIRROR_COEFF_T)));
		laserpower    = Double.parseDouble(properties.getProperty(LASER_POWER,        Double.toString(DEFAULT_LASER_POWER)));
		tCameraFPS    = Double.parseDouble(properties.getProperty(CAMERA_T_FRAMERATE, Double.toString(DEFAULT_CAMERA_T_FRAMERATE)));
		tCameraExp    = Double.parseDouble(properties.getProperty(CAMERA_T_EXPOSURE,  Double.toString(DEFAULT_CAMERA_T_EXPOSURE)));
		tCameraGain   = Integer.parseInt(  properties.getProperty(CAMERA_T_GAIN,      Integer.toString(DEFAULT_CAMERA_T_GAIN)));
		fCameraFPS    = Double.parseDouble(properties.getProperty(CAMERA_F_FRAMERATE, Double.toString(DEFAULT_CAMERA_F_FRAMERATE)));
		fCameraExp    = Double.parseDouble(properties.getProperty(CAMERA_F_EXPOSURE,  Double.toString(DEFAULT_CAMERA_F_EXPOSURE)));
		fCameraGain   = Integer.parseInt(  properties.getProperty(CAMERA_F_GAIN,      Integer.toString(DEFAULT_CAMERA_F_GAIN)));
		pixelWidth    = Double.parseDouble(properties.getProperty(PIXEL_WIDTH,        Double.toString(DEFAULT_PIXEL_WIDHT)));
		stacksdir     = properties.getProperty(STACKS_DIR,         DEFAULT_STACKS_DIR);
		logsdir       = properties.getProperty(LOGS_DIR,           DEFAULT_LOGS_DIR);
		propertiesdir = properties.getProperty(PROPERTIES_DIR,     DEFAULT_PROPERTIES_DIR);
		statisticspath= properties.getProperty(STATISTICS_PATH,    DEFAULT_STATISTICS_PATH);
		snapshotpath  = properties.getProperty(SNAPSHOT_PATH,      DEFAULT_SNAPSHOT_PATH);
		stackslink    = properties.getProperty(STACKS_LINK,        DEFAULT_STACKS_LINK);
		logslink      = properties.getProperty(LOGS_LINK,          DEFAULT_LOGS_LINK);
		statisticslink= properties.getProperty(STATISTICS_LINK,    DEFAULT_STATISTICS_LINK);
		mailto        = properties.getProperty(MAIL_TO,            DEFAULT_MAIL_TO);
		mailcc        = properties.getProperty(MAIL_CC,            DEFAULT_MAIL_CC);
		mailuser      = properties.getProperty(MAIL_SMTP_USER,     DEFAULT_MAIL_SMTP_USER);
		mailpassword  = properties.getProperty(MAIL_SMTP_PASSWORD, DEFAULT_MAIL_SMTP_PASSWORD);
		failWithoutArduino = Boolean.parseBoolean(properties.getProperty(FAIL_WITHOUT_ARDUINO, Boolean.toString(DEFAULT_FAIL_WITHOUT_ARDUINO)));
		simulating    = Boolean.parseBoolean(properties.getProperty(SIMULATING, Boolean.toString(DEFAULT_SIMULATING)));
	}

	private void write() {
		write(propertiesFile);
	}

	private void write(File file) {
		File parent = file.getParentFile();
		if(!parent.exists())
			parent.mkdirs();
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

	private void doWrite(PrintWriter out) {
		out.println("# Properties for EduSPIM");
		out.println();
		out.println("# Volume limits:");
		out.println(STACK_Y_START + "=" + stackYStart);
		out.println(STACK_Z_START + "=" + stackZStart);
		out.println(STACK_Y_END   + "=" + stackYEnd);
		out.println(STACK_Z_END   + "=" + stackZEnd);
		out.println();
		out.println("# Mirror calibration");
		out.println(MIRROR_Z1 + "=" + mirrorZ1);
		out.println(MIRROR_M1 + "=" + mirrorM1);
		out.println(MIRROR_Z2 + "=" + mirrorZ2);
		out.println(MIRROR_M2 + "=" + mirrorM2);
		out.println("# Resulting offset and slope");
		out.println(MIRROR_COEFF_T + "=" + mirrorCoeffT);
		out.println(MIRROR_COEFF_M + "=" + mirrorCoeffM);
		out.println();
		out.println("# Laser power, in mW");
		out.println(LASER_POWER + "=" + laserpower);
		out.println();
		out.println("# Settings for the fluorescence camera");
		out.println(CAMERA_F_FRAMERATE + "=" + fCameraFPS);
		out.println(CAMERA_F_EXPOSURE  + "=" + fCameraExp);
		out.println(CAMERA_F_GAIN      + "=" + fCameraGain);
		out.println();
		out.println("# Settings for the transmission camera");
		out.println(CAMERA_T_FRAMERATE + "=" + tCameraFPS);
		out.println(CAMERA_T_EXPOSURE  + "=" + tCameraExp);
		out.println(CAMERA_T_GAIN      + "=" + tCameraGain);
		out.println();
		out.println("# Physical width of a pixel, in mm");
		out.println(PIXEL_WIDTH + "=" + pixelWidth);
		out.println();
		out.println("# Folder where stack projections are written to");
		out.println(STACKS_DIR + "=" + escape(stacksdir));
		out.println();
		out.println("# File to which the current snapshot is written to");
		out.println(SNAPSHOT_PATH + "=" + escape(snapshotpath));
		out.println();
		out.println("# Folder where logs are written to");
		out.println(LOGS_DIR + "=" + escape(logsdir));
		out.println();
		out.println("# Folder where properties files are written to on change");
		out.println(PROPERTIES_DIR + "=" + escape(propertiesdir));
		out.println();
		out.println("# Path to a statistics file");
		out.println(STATISTICS_PATH + "=" + escape(statisticspath));
		out.println();
		out.println("# Links that point a browser to shared logs/snapshots directories,");
		out.println("# included in some emails");
		out.println(STACKS_LINK + "=" + escape(stackslink));
		out.println(LOGS_LINK + "=" + escape(logslink));
		out.println(STATISTICS_LINK + "=" + escape(statisticslink));
		out.println();
		out.println("# Email addresses");
		out.println(MAIL_TO + "=" + escape(mailto));
		out.println(MAIL_CC + "=" + escape(mailcc));
		out.println(MAIL_SMTP_USER + "=" + escape(mailuser));
		out.println(MAIL_SMTP_PASSWORD + "=" + escape(mailpassword));
		out.println();
		out.println("# Fails if communication to the arduino cannot be established.");
		out.println("# If false, GUI buttons will be used instead.");
		out.println(FAIL_WITHOUT_ARDUINO + "=" + failWithoutArduino);
		out.println();
		out.println("# Specify whether normal operation should be simulated using pre-acquired data");
		out.println(SIMULATING + "=" + simulating);
	}

	private static String escape(String s) {
		while(s.endsWith("\\"))
			s = s.substring(0, s.length() - 1);
		return s.replaceAll("\\\\", "\\\\\\\\")
				.replaceAll(":", "\\\\:")
				.replaceAll("=", "\\\\=");
	}
}

