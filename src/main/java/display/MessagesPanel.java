package display;

import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;

public class MessagesPanel extends JEditorPane {

	private ArrayList<Line> lines = new ArrayList<Line>();

	public MessagesPanel() {
		super("text/html", "");
		setBorder(BorderFactory.createEmptyBorder(200, 100, 50, 100));
		setBackground(Color.BLACK);
		setForeground(Color.white);
		lines.add(new Line());
//		setText(
//				"<html>" +
//				"  <p style=\"font-size:15px; color: #a0a0a0\">" +
//				"    <br><br>" +
//				"    Das Mikroskop ist zur Zeit wegen eines Hardwarefehlers ausser Betrieb und " +
//				"    wird in Kuerze gewartet.<br>" +
//				"    Wir bitten um Ihr Verstaendnis." +
//				"  </p>" +
//				"</html>");
	}

	private String makeHelpText() {
		String mod = IJ.isMacOSX() ? "Cmd" : "Ctrl";
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("  <div style=\"font-size:15px; color: #ffffff;\">");
		sb.append("<table cellspacing=\"8px\">");
		sb.append("  <tr>");
		sb.append("    <th style=\"text-align: left;\">Shortcut</th>");
		sb.append("    <th style=\"text-align: left;\">Description</th>");
		sb.append("  </tr>");
		sb.append("  <tr>");
		sb.append("    <td>" + mod + "-h</td>");
		sb.append("    <td>Show this help</td>");
		sb.append("  </tr>");
		sb.append("  <tr>");
		sb.append("    <td>" + mod + "-f</td>");
		sb.append("    <td>Toggle fullscreen</td>");
		sb.append("  </tr>");
		sb.append("  <tr>");
		sb.append("    <td>" + mod + "-q</td>");
		sb.append("    <td>Quit the application</td>");
		sb.append("  </tr>");
		sb.append("  <tr>");
		sb.append("    <td>" + mod + "-a</td>");
		sb.append("    <td>Show the administrator panel</td>");
		sb.append("  </tr>");
		sb.append("  <tr>");
		sb.append("    <td>" + mod + "-b</td>");
		sb.append("    <td>Show the BeanShell panel</td>");
		sb.append("  </tr>");
		sb.append("  <tr>");
		sb.append("    <td>" + mod + "-v</td>");
		sb.append("    <td>Toggle simulating mode</td>");
		sb.append("  </tr>");
		sb.append("</table>");
		sb.append("  </div>");
		sb.append("</html>");
		return sb.toString();
	}

	public void showHelp() {
		setMessage(makeHelpText());
	}

	private String makeHTML() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		for(Line  l : lines) {
			sb.append("  <div style=\"font-size:15px; padding-top: 5px; color: #a0a0a0\">");
			for(Token tok : l) {
				sb.append("<span " + tok.style + ">");
				sb.append(tok.s);
				sb.append("</span>");
			}
			sb.append("  </div>");
		}
		sb.append("  </div>");
		sb.append("</html>");
		return sb.toString();
	}

	private void update() {
		setText(makeHTML());
	}

	public void println(String line) {
		println(line, Color.WHITE, Font.PLAIN);
	}

	public void println(String line, Color c, int type) {
		lines.get(lines.size() - 1).add(line, c, type);
		lines.add(new Line());
		update();
	}

	public void print(String s, Color c, int type) {
		lines.get(lines.size() - 1).add(s, c, type);
		update();
	}

	public void print(String line) {
		print(line, Color.WHITE, Font.PLAIN);
	}

	public void failed() {
		println("Failed", Color.RED, Font.BOLD);
	}

	public void succeeded() {
		println("OK", Color.GREEN, Font.BOLD);
	}

	public void setMessage(String message) {
		setText(message);
	}

	private static class Line implements Iterable<Token> {

		private ArrayList<Token> tokens = new ArrayList<Token>();

		void add(String s, Color c, int type) {
			tokens.add(new Token(s, c, type));
		}

		void clear() {
			tokens.clear();
		}

		@Override
		public Iterator<Token> iterator() {
			return tokens.iterator();
		}
	}

	private static class Token {

		private final String s;
		private final int type;
		private final String color;
		private final String style;

		public Token(String s) {
			this(s, Color.black);
		}

		private String makeStyle() {
			String fontstyle = type == Font.ITALIC ? "italic" : "normal";
			String fontweight = type == Font.BOLD ? "bold" : "normal";
			return "style=\"color:" + color +
					"; font-style: " + fontstyle +
					"; font-weight: " + fontweight +
					"; line-height: 500px" +
					";\"";
		}

		public Token(String s, Color c) {
			this(s, c, Font.PLAIN);
		}

		public Token(String s, Color c, int type) {
			this.s = s;
			this.color = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
			this.type = type;
			this.style = makeStyle();
			System.out.println(style);
		}
	}

	public static void main(String[] args) throws InterruptedException {
		System.out.print("bla");
		System.out.println("blubb");
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		MessagesPanel mp = new MessagesPanel();
		frame.getContentPane().add(mp, BorderLayout.CENTER);
		frame.pack();
		frame.setSize(800, 600);
		frame.setVisible(true);

		mp.showHelp();

//		for(int i = 0; i < 10; i++) {
//			Thread.sleep(1000);
//			mp.print("Trying to connect motors...   ");
//			Thread.sleep(1000);
//			mp.failed();
//			Thread.sleep(1000);
//			mp.print("Trying to connect camera...   ");
//			Thread.sleep(1000);
//			mp.succeeded();
//		}
	}
}
