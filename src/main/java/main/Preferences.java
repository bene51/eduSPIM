package main;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import stage.IMotor;

public class Preferences {

	public static final String PROPERTY_FILE =
		System.getProperty("user.home") + File.separator + ".EduSPIM.props";

	public static final String STACK_Z_START = "stack_z_start";
	public static final String STACK_Z_END   = "stack_z_end";
	public static final String STACK_Y_START = "stack_y_start";
	public static final String STACK_Y_END   = "stack_y_end";

	private static Preferences instance;

	private final Properties properties;
	private final File propertiesFile;

	private double stackZStart, stackZEnd, stackYStart, stackYEnd;

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

	private Preferences() {
		properties = new Properties();

		propertiesFile = new File(PROPERTY_FILE);

		FileReader reader = null;
		try {
			reader = new FileReader(propertiesFile);
			properties.load(reader);
		} catch(Exception e) {
			e.printStackTrace();
			properties.put(STACK_Z_START, Double.toString(IMotor.POS_MIN_Z));
			properties.put(STACK_Z_END,   Double.toString(IMotor.POS_MAX_Z));
			properties.put(STACK_Y_START, Double.toString(IMotor.POS_MIN_Y));
			properties.put(STACK_Y_END,   Double.toString(IMotor.POS_MAX_Y));
			save(propertiesFile, properties);
		} finally {
			try {
				reader.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		stackZStart = Double.parseDouble(properties.getProperty(STACK_Z_START));
		stackZEnd   = Double.parseDouble(properties.getProperty(STACK_Z_END));
		stackYStart = Double.parseDouble(properties.getProperty(STACK_Y_START));
		stackYEnd   = Double.parseDouble(properties.getProperty(STACK_Y_END));
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

	private static void set(String key, Object value) {
		getInstance().properties.put(key, value.toString());
		save(getInstance().propertiesFile, getInstance().properties);
	}

	private static void save(File file, Properties props) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(file);
			props.store(writer, "EduSPIM properties");
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}

