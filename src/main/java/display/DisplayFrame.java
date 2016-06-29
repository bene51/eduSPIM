package display;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import buttons.AWTButtons;

@SuppressWarnings("serial")
public class DisplayFrame extends JFrame {

	private boolean fullscreen = false;

	private final JLabel busy;
	private final JLabel message;
	private boolean simulated = false;
	private PlaneDisplay planeDisplay;
	private MessagesPanel messages;

	private OverviewPanel overview;

	public DisplayFrame(boolean fatal) {
		super("Display");
		getContentPane().setLayout(new BorderLayout());
		setBackground(Color.BLACK);

		messages = new MessagesPanel();

		getContentPane().add(
				fatal ? makeUnavailablePanel() : messages, BorderLayout.CENTER);

		busy = new JLabel("  ");
		busy.setPreferredSize(new Dimension(200, 15));
		busy.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		busy.setForeground(Color.RED);
		busy.setBackground(Color.BLACK);
		busy.setOpaque(true);
		getContentPane().add(busy, BorderLayout.SOUTH);

		int avWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
		int fontsize = 12;
		if(avWidth > 1300)
			fontsize = 13;
		if(avWidth > 1500)
			fontsize = 14;
		if(avWidth > 1700)
			fontsize = 15;
		if(avWidth > 1900)
			fontsize = 16;

		message = new JLabel(
				" \n" +
				" Please note, that the sample currently needs to be exchanged" +
				" before live view is possible again. To still provide you with" +
				" the same user experience, data is shown that has been acquired" +
				" previously on this system.");
		message.setFont(new Font("Helvetica", Font.PLAIN, fontsize));
		message.setPreferredSize(new Dimension(200, 40));
		message.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		message.setBackground(Color.BLACK);// new Color(255, 80, 60));
		message.setForeground(new Color(255, 255, 200));
		message.setOpaque(true);
		if(simulated)
			getContentPane().add(message, BorderLayout.NORTH);
	}

	public boolean isHelpVisible() {
		return messages == ((BorderLayout)getContentPane().
				getLayout()).getLayoutComponent(BorderLayout.CENTER);
	}

	public MessagesPanel getMessages() {
		return messages;
	}

	public void setPlaneDisplay(PlaneDisplay disp) {
		this.planeDisplay = disp;
		showPlaneDisplay();
	}

	public void showHelp() {
		System.out.println("showHelp");
		messages.showHelp();
		BorderLayout layout = (BorderLayout)getContentPane().getLayout();
		getContentPane().remove(layout.getLayoutComponent(BorderLayout.CENTER));
		getContentPane().add(messages, BorderLayout.CENTER);
		messages.invalidate();
		messages.repaint();
	}

	public void showPlaneDisplay() {
		System.out.println("showPlaneDisplay");
		BorderLayout layout = (BorderLayout)getContentPane().getLayout();
		getContentPane().remove(layout.getLayoutComponent(BorderLayout.CENTER));
		getContentPane().add(planeDisplay, BorderLayout.CENTER);
		planeDisplay.invalidate();
		planeDisplay.repaint();
		this.invalidate();
		this.validate();
	}

	public void showFatal() {
		BorderLayout layout = (BorderLayout)getContentPane().getLayout();
		getContentPane().remove(layout.getLayoutComponent(BorderLayout.CENTER));
		getContentPane().add(makeUnavailablePanel(), BorderLayout.CENTER);
	}

	public void makeOverviewPanel(final AWTButtons buttons) {
		if(overview != null) {
			System.out.println("overview already exists");
			return;
		}

		overview = new OverviewPanel(new Overview3D(), buttons);
		int ow = getWidth() / 5; // panel.getPreferredSize().width / 4;
		overview.setPreferredSize(new Dimension(ow, overview.getHeight()));
		getContentPane().add(overview, BorderLayout.EAST);

		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				overview.setPreferredSize(new Dimension(getWidth() / 5, overview.getHeight()));
				if(buttons!= null) {
					buttons.updatePreferredSize(getWidth() / 5); // panel.getWidth() / 4);
					buttons.getPanel().revalidate();
				}
				overview.revalidate();
			}

			@Override public void componentMoved(ComponentEvent e) {}
			@Override public void componentShown(ComponentEvent e) {}
			@Override public void componentHidden(ComponentEvent e) {}

		});
		this.invalidate();
		this.validate();
	}

	public void updateOverview(double yrel, double zrel) {
		overview.setPosition(yrel, zrel);
	}

	public void showSimulatedMessage(boolean b) {
		if(b && !simulated) {
			getContentPane().add(message, BorderLayout.NORTH);
			validate();
		}
		else if(!b && simulated) {
			getContentPane().remove(message);
			validate();
		}
		simulated = b;
	}

	public JComponent makeUnavailablePanel() {
		String text =
				"<html>" +
						"  <h1 style=\"font-size:30px; color: #ffcc00\">Ausser Betrieb / Unavailable</h1>" +
						"  <p style=\"font-size:15px; color: #a0a0a0\">" +
						"    <br><br>" +
						"    Das Mikroskop ist zur Zeit wegen eines Hardwarefehlers ausser Betrieb und " +
						"    wird in Kuerze gewartet.<br>" +
						"    Wir bitten um Ihr Verstaendnis." +
						"  </p>" +
						"  <br><br>" +
						"  <p style=\"font-size:15px; color: #a0a0a0\">" +
						"    The microscope is currently unavailable due to hardware issues. It will be " +
						"    maintained shortly.<br>" +
						"    Please apologize this incident." +
						"  </p>" +
						"</html>";
		JEditorPane panel = new JEditorPane("text/html", text);
		panel.setBorder(BorderFactory.createEmptyBorder(200, 100, 50, 100));
		panel.setBackground(Color.BLACK);
		panel.setForeground(Color.WHITE);
		return panel;
	}

	public void showBusy(String message) {
		this.busy.setText(message);
		this.busy.repaint();
	}

	public void clearBusy() {
		this.busy.setText("");
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

	public void setFullscreen(boolean fullscreen) {
		setVisible(false);
		dispose();
		this.setUndecorated(fullscreen);
		setResizable(!fullscreen);
		if (fullscreen)
			setExtendedState(MAXIMIZED_BOTH);
		else
			setExtendedState(NORMAL);
		this.fullscreen = fullscreen;
		revalidate();
		setVisible(true);
	}

	/*
	 * Just here as a reference, not used.
	 */
	void setRealFullscreen(boolean fullscreen) {
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
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				DisplayFrame f = new DisplayFrame(true);
				f.pack();
				f.setVisible(true);
				// f.setFullscreen(true);
				// f.setRealFullscreen(true);
			}
		});
	}
}
