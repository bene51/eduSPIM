package main;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHandler {

	public static void handleException(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		Mail.sendError(sw.toString());
		// TODO also log it
	}

	public static void showException(Throwable e) {
		e.printStackTrace();
		// TODO add implementation
	}

	public static void main(String[] args) {
		Exception e = new RuntimeException("bla");
		try {
			throw(e);
		} catch(Exception ex) {
			handleException(ex);
		}
	}
}
