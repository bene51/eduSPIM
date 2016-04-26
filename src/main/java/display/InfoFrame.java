package display;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

@SuppressWarnings("serial")
public class InfoFrame extends JFrame {

	public InfoFrame() {
		super("Info");
		ImageIcon icon = makeInfoIcon();
		getContentPane().add(new JLabel(icon));
		getContentPane().setBackground(Color.BLACK);

		// set pseudo-fullscreen
		this.setUndecorated(true);
		setResizable(false);
		setExtendedState(MAXIMIZED_BOTH);
		setVisible(true);
	}

	private ImageIcon makeInfoIcon() {
		URL url = getClass().getResource("/eduspim_infopage.png");
		Image image = Toolkit.getDefaultToolkit().getImage(url);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		image = image.getScaledInstance(dim.width, dim.height, Image.SCALE_SMOOTH);
		ImageIcon icon = new ImageIcon(image);
		return icon;
	}

	public static void main(String[] args) throws InterruptedException {
		InfoFrame f = new InfoFrame();
		Thread.sleep(10000);
		f.dispose();
	}
}
