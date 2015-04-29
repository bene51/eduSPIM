package display;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class DisplayFrame extends JFrame {

	private boolean fullscreen = false;

	private final JLabel message;

	public DisplayFrame(PlaneDisplay disp, boolean fatal) {
		super("Display");
		JPanel panel = fatal ? makeUnavailablePanel() : disp;
		getContentPane().add(panel);
		setFocusable(false);

		message = new JLabel("");
		message.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		message.setForeground(Color.WHITE);
		message.setBackground(Color.BLACK);
		message.setOpaque(true);
		getContentPane().add(message, BorderLayout.SOUTH);
	}

	public JPanel makeUnavailablePanel() {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(200, 100, 50, 100));
		panel.setBackground(Color.BLACK);
		JLabel label = new JLabel();
		label.setForeground(Color.WHITE);
		label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		label.setAlignmentY(JLabel.CENTER_ALIGNMENT);
		label.setText(
				"<html><h1 style=\"font-size:30px; color: #ffcc00\">Ausser Betrieb / Unavailable</h1>" +
				"<p style=\"font-size:15px; color: #a0a0a0\"><br><br>Das Mikroskop ist zur Zeit wegen eines Hardwarefehlers ausser Betrieb und wird in Kuerze gewartet.<br>Wir bitten um Ihr Verstaendnis.</p><br><br>" +
				"<p style=\"font-size:15px; color: #a0a0a0\">The microscope is currently unavailable due to hardware issues. It will be maintained shortly.<br>Please apologize this accident.</p></html>");
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}

	public void showMessage(String message) {
		this.message.setText(message);
		this.message.repaint();
	}

	public void clearMessage() {
		this.message.setText("");
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

	public void setFullscreen(boolean fullscreen) {
		GraphicsEnvironment env = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice device = env.getDefaultScreenDevice();

		if (fullscreen) {
			if (device.isFullScreenSupported()) {
				setVisible(false);
				dispose();
				this.setUndecorated(true);
				device.setFullScreenWindow(this);
				this.fullscreen = true;
				setVisible(true);
			}
		} else {
			setVisible(false);
			dispose();
			setUndecorated(false);
			device.setFullScreenWindow(null);
			this.fullscreen = false;
			setVisible(true);
		}
	}

	public static void main(String[] args) {
		DisplayFrame f = new DisplayFrame(null, true);
		f.pack();
		f.setVisible(true);
		f.setFullscreen(true);
	}
}
