package main;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ExceptionHandler {

	public static void handleException(Throwable e) {
		Mail.sendError(exceptionToString(e));
		// TODO also log it
	}

	private static String exceptionToString(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}

	public static void showException(Throwable e) {
		e.printStackTrace();
		String s = exceptionToString(e);
		JTextArea ta = new JTextArea(s);
		final JComponent[] inputs = new JComponent[] { new JScrollPane(ta) };
		JOptionPane.showMessageDialog(null, inputs, "Exception", JOptionPane.ERROR_MESSAGE);
	}

	public static void main(String[] args) {
		Exception e = new RuntimeException("bla");
		try {
			throw(e);
		} catch(Exception ex) {
			// handleException(ex);
			showException(ex);
		}
	}
}
