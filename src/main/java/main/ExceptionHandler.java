package main;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

	public static void handleException(String message, Throwable e) {
		e.printStackTrace();
		Mail.sendError(exceptionToString(e));
		logger.error(message, e);
	}

	private static String exceptionToString(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}

	public static void showException(String message, Throwable e) {
		logger.error(message, e);
		String s = exceptionToString(e);
		JTextArea ta = new JTextArea(message + "\n\n" + s);
		final JComponent[] inputs = new JComponent[] { new JScrollPane(ta) };
		JOptionPane.showMessageDialog(null, inputs, "Exception", JOptionPane.ERROR_MESSAGE);
	}

	public static void main(String[] args) {

		Exception e = new RuntimeException("bla");
		try {
			throw(e);
		} catch(Exception ex) {
			// handleException(ex);
			showException("Error in main", ex);
		}
	}
}
