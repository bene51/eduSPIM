package buttons;

import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class AWTButtons extends AbstractButtons {

	private final Panel panel = new Panel(new GridLayout(2,3,2,2));

	private final Button yDownButton = new Button("y-");
	private final Button yUpButton   = new Button("y+");
	private final Button zDownButton = new Button("z-");
	private final Button zUpButton   = new Button("z+");
	private final Button stackButton = new Button("S");
	private final Button laserButton = new Button("L");

	public AWTButtons() {
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		panel.setLayout(gridbag);

		panel.setBackground(Color.BLACK);
		panel.setForeground(Color.white);

		c.anchor = GridBagConstraints.CENTER;
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

	public Panel getPanel() {
		return panel;
	}

	private class MouseAdapter implements MouseListener {

		private final int button;

		MouseAdapter(int button) {
			this.button = button;
		}

		public void mouseClicked(MouseEvent e) {}

		public void mousePressed(MouseEvent e) {
			fireButtonPressed(button);
		}

		public void mouseReleased(MouseEvent e) {
			fireButtonReleased(button);
		}

		public void mouseEntered(MouseEvent e) {}

		public void mouseExited(MouseEvent e) {}

	}

	public static void main(String... args) {
		Frame f = new Frame();
		f.add(new AWTButtons().getPanel());
		f.pack();
		f.setVisible(true);
	}

}
