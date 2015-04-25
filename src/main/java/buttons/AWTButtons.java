package buttons;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class AWTButtons extends AbstractButtons {

	private final JPanel panel = new JPanel(new GridLayout(2,3,1,1));

	private final JButton yDownButton = new MButton("y-");
	private final JButton yUpButton   = new MButton("y+");
	private final JButton zDownButton = new MButton("z-");
	private final JButton zUpButton   = new MButton("z+");
	private final JButton stackButton = new MButton("S");
	private final JButton laserButton = new MButton("L");

	public AWTButtons() {
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		panel.setLayout(gridbag);

		panel.setBackground(Color.BLACK);
		panel.setForeground(Color.white);

		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 0, 0, 0);

		c.gridx = 1;
		c.gridy = 0;
		panel.add(yUpButton, c);

		c.gridx = 1;
		c.gridy = 1;
		panel.add(yDownButton, c);

		c.gridx = 0;
		c.gridy = 1;
		panel.add(zDownButton, c);

		c.gridx = 2;
		c.gridy = 1;
		panel.add(zUpButton, c);

		c.gridy = 0;
		c.gridx = 3;
		c.insets = new Insets(0, 7, 0, 7);
		panel.add(stackButton, c);

		c.gridy = 1;
		c.gridx = 3;
		panel.add(laserButton, c);

		yDownButton.addMouseListener(new MouseAdapter(BUTTON_Y_DOWN));
		yUpButton.addMouseListener(new MouseAdapter(BUTTON_Y_UP));
		zDownButton.addMouseListener(new MouseAdapter(BUTTON_Z_DOWN));
		zUpButton.addMouseListener(new MouseAdapter(BUTTON_Z_UP));
		stackButton.addMouseListener(new MouseAdapter(BUTTON_STACK));
		laserButton.addMouseListener(new MouseAdapter(BUTTON_LASER));
	}

	public JPanel getPanel() {
		return panel;
	}

	@SuppressWarnings("serial")
	private static class MButton extends JButton {

		public MButton(String s) {
			super(s);
			setForeground(Color.BLACK);
			Dimension d = getPreferredSize();
			this.setMargin(new Insets(1, 1, 1, 1));
			setPreferredSize(new Dimension(3 * d.height / 2, d.height));
		}
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
