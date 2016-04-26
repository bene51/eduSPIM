package display;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import buttons.AWTButtons;

@SuppressWarnings("serial")
public class OverviewPanel extends JPanel {

	private final Overview3D overview;
	private double yrel = 0, zrel = 0;
	private BufferedImage buttonLabels;

	public OverviewPanel(Overview3D overview, AWTButtons buttons) {
		super();
		this.overview = overview;
		setBackground(Color.BLACK);
		setOpaque(true);

		setLayout(new BorderLayout());
		if(buttons != null)
			add(buttons.getPanel(), BorderLayout.SOUTH);

		if(buttons == null) {
			InputStream is = getClass().getResourceAsStream("/Buttons_Labels.png");
			BufferedImage bi = null;
			if(is != null) {
				try {
					bi = ImageIO.read(is);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						is.close();
					} catch(Exception e) {}
				}
			}
			buttonLabels = bi == null ? null : bi;
		} else {
			buttonLabels = null;
		}
	}

	public void setPosition(double yrel, double zrel) {
		this.yrel = yrel;
		this.zrel = zrel;
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		System.out.println("OverviewPanel.paintComponent()");
		super.paintComponent(g);
		int x = (getWidth() - overview.getWidth()) / 2;
		int y = 20;
		g.drawImage(overview.get(yrel, zrel), x, y, null);

		if(buttonLabels != null) {
			int ow = buttonLabels.getWidth();
			int oh = buttonLabels.getHeight();

			int cw = getWidth();
			int ch;

			if(cw > ow) {
				x = Math.round((cw - ow) / 2);
				cw = ow;
				ch = oh;
			} else {
				// xOffs and cw are fine
				x = 0;
				ch = cw * oh / ow;
			}
			System.out.println(x + ", " + ch + ", " + cw);
			g.drawImage(buttonLabels, x, getHeight() - ch, cw, ch, null);
		}
	}

	public static void main(String[] args) {
		final JFrame f = new JFrame();
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(new JPanel(), BorderLayout.CENTER);

		final OverviewPanel overview = new OverviewPanel(new Overview3D(), new AWTButtons());
		f.getContentPane().add(overview, BorderLayout.EAST);
		f.addComponentListener(new ComponentListener() {
			@Override
			public void componentResized(ComponentEvent e) {
				Container c = f.getContentPane();
				System.out.println("w = " + c.getWidth() + ", h = " + c.getHeight());
				overview.setPreferredSize(new Dimension(c.getWidth() / 5, overview.getHeight()));
			}

			@Override public void componentMoved(ComponentEvent e) {}
			@Override public void componentShown(ComponentEvent e) {}
			@Override public void componentHidden(ComponentEvent e) {}

		});
		f.pack();
		f.setVisible(true);
	}
}
