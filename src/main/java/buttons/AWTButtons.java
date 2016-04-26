package buttons;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class AWTButtons extends AbstractButtons {

	private final int ins = 10;

	private final JPanel panel = new JPanel();

	private final NiceButton yDownButton = new NiceButton(0);
	private final NiceButton yUpButton   = new NiceButton(1);
	private final NiceButton zDownButton = new NiceButton(2);
	private final NiceButton zUpButton   = new NiceButton(3);
	private final NiceButton stackButton = new NiceButton(4);
	private final NiceButton laserButton = new NiceButton(5);
	private final NiceButton infoButton  = new NiceButton(6);

	public AWTButtons() {
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		panel.setLayout(gridbag);

		panel.setBackground(Color.BLACK);
		panel.setForeground(Color.white);
		panel.setOpaque(true);

		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(3 * ins / 2, ins, 3 * ins / 2, ins);

		c.gridx = 0;
		c.gridy = 0;
		panel.add(yUpButton, c);

		c.gridx = 0;
		c.gridy = 1;
		panel.add(yDownButton, c);

		c.gridx = 1;
		c.gridy = 0;
		panel.add(zUpButton, c);

		c.gridx = 1;
		c.gridy = 1;
		panel.add(zDownButton, c);

		c.insets = new Insets(3 * ins / 2, 4 * ins, 3 * ins / 2, ins);

		c.gridx = 2;
		c.gridy = 0;
		panel.add(laserButton, c);

		c.gridx = 2;
		c.gridy = 1;
		panel.add(stackButton, c);

		c.insets = new Insets(3 * ins / 2, ins, 3 * ins / 2, ins);
		c.gridy = 0;
		c.gridx = 3;
		panel.add(infoButton, c);

		yDownButton.addMouseListener(new MouseAdapter(BUTTON_Y_DOWN));
		yUpButton.addMouseListener(new MouseAdapter(BUTTON_Y_UP));
		zDownButton.addMouseListener(new MouseAdapter(BUTTON_Z_DOWN));
		zUpButton.addMouseListener(new MouseAdapter(BUTTON_Z_UP));
		stackButton.addMouseListener(new MouseAdapter(BUTTON_STACK));
		laserButton.addMouseListener(new MouseAdapter(BUTTON_LASER));
		infoButton.addMouseListener(new MouseAdapter(BUTTON_INFO));

		panel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent arg0) {
				updatePreferredSize(panel.getWidth());
			}
		});
	}

	public void updatePreferredSize(int rw) {
		float tw = ins * 11 + 4 * yDownButton.getPreferredSize().width;
		float th = ins * 6  + 2 * yDownButton.getPreferredSize().height;
		System.out.println("tw x th = " + tw + " x " + th);
	    Rectangle b = panel.getBounds();
	    // nw / nh = tw / th
	    float hfactor = rw  / tw;
	    float factor = hfactor; // Math.min(hfactor, vfactor);
	    int nw = Math.round(tw * factor);
	    int nh = Math.round(th * factor);
	    if(nw != b.width || nh != b.height) {
	    	System.out.println("x");
	    	panel.setPreferredSize(new Dimension(nw, nh));
	    	panel.revalidate();
	    }
	}

	@Override
	public void close() {}

	public JPanel getPanel() {
		return panel;
	}

	private class MouseAdapter implements MouseListener {

		private final int button;

		MouseAdapter(int button) {
			this.button = button;
		}

		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {
			fireButtonPressed(button);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			fireButtonReleased(button);
		}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

	}

	public static void main(String... args) {
		JFrame f = new JFrame();
		f.getContentPane().add(new AWTButtons().getPanel());
		f.pack();
		f.setVisible(true);
	}

}
