package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Statistics {

	public static final char DELIMITER = '\t';

	public static final String HEADER =
			"Date"    + DELIMITER +
			"#Stacks" + DELIMITER +
			"#Moves"  + DELIMITER +
			"#Lasers" + DELIMITER +
			"#Infos"  + DELIMITER +
			"Sample_change";


	public static void incrementStacks() {
		try {
			getInstance().getTodaysEntry().stacks++;
			getInstance().save();
		} catch(Exception e) {
			ExceptionHandler.handleException("Error incrementing #Stacks for statistics", e);
		}
	}

	public static void incrementMoves() {
		try {
			getInstance().getTodaysEntry().moves++;
			getInstance().save();
		} catch(Exception e) {
			ExceptionHandler.handleException("Error incrementing #Moves for statistics", e);
		}
	}

	public static void incrementLasers() {
		try {
			getInstance().getTodaysEntry().lasers++;
			getInstance().save();
		} catch(Exception e) {
			ExceptionHandler.handleException("Error incrementing #Lasers for statistics", e);
		}
	}

	public static void incrementInfos() {
		try {
			getInstance().getTodaysEntry().infos++;
			getInstance().save();
		} catch(Exception e) {
			ExceptionHandler.handleException("Error incrementing #Infos for statistics", e);
		}
	}

	public static void changeSample() {
		try {
			getInstance().getTodaysEntry().sampleChange = true;
			getInstance().save();
		} catch(Exception e) {
			ExceptionHandler.handleException("Error setting 'Sample change' for statistics", e);
		}
	}

	private ArrayList<Entry> allEntries;
	private Entry today;

	private static Statistics instance = null;

	private Statistics() throws Exception {
		load();
		instance = this;
	}

	private static Statistics getInstance() throws Exception {
		if(instance == null)
			instance = new Statistics();
		return instance;
	}

	private void load() throws Exception {
		allEntries = new ArrayList<Entry>();
		String path = Preferences.getStatisticsPath();
		File f = new File(path);
		if(!f.exists()) {
			Mail.send("eduSPIM statistics file not found",
						Preferences.getMailto(),
						null, // CC
						"Couldn't find statistics file at\n" + path +
						"\nCreating new file");
		} else {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			reader.readLine(); // ignore the header line
			String line;
			while((line = reader.readLine()) != null) {
				allEntries.add(Entry.parseLine(line));
			}
			reader.close();
			if(allEntries.size() > 0)
				today = allEntries.get(allEntries.size() - 1);
		}
	}

	private void save() {
		String path = Preferences.getStatisticsPath();
		PrintWriter out = null;
		try {
			out = new PrintWriter(new File(path));
			doWrite(out);
		} catch(Exception e) {
			ExceptionHandler.handleException("Error saving statistics file", e);
		} finally {
			try {
				if(out != null)
					out.close();
			} catch(Exception e) {
				ExceptionHandler.handleException("Error closing statistics file", e);
			}
		}
	}

	private void doWrite(PrintWriter out) {
		out.println(HEADER);
		for(Entry entry : allEntries)
			out.println(entry.toLine());
		out.close();
	}

	public Entry getTodaysEntry() {
		String date = new SimpleDateFormat("yyyMMdd").format(new Date());
		if(today == null || !today.date.equals(date)) {
			today = new Entry();
			today.date = date;
			allEntries.add(today);
		}
		return today;
	}

	private static final class Entry {
		private String date = "";
		private int stacks = 0;
		private int moves = 0;
		private int lasers = 0;
		private int infos = 0;
		private boolean sampleChange = false;

		public String toLine() {
			return date    + DELIMITER +
					stacks + DELIMITER +
					moves  + DELIMITER +
					lasers + DELIMITER +
					infos  + DELIMITER +
					(sampleChange ? 1 : 0);
		}

		public static Entry parseLine(String line) throws Exception { // TODO make more specific exception
			Entry entry = new Entry();
			String[] toks = line.split("\t");
			try {
				entry.date = toks[0];
				entry.stacks       = Integer.parseInt(toks[1]);
				entry.moves        = Integer.parseInt(toks[1]);
				entry.lasers       = Integer.parseInt(toks[1]);
				entry.infos        = Integer.parseInt(toks[1]);
				entry.sampleChange = Integer.parseInt(toks[1]) > 0;
				return entry;
			} catch(RuntimeException e) {
				throw new Exception("Error parsing line of statistics file: " + line, e);
			}
		}
	}
}
