package buttons;

import java.awt.Button;
import java.awt.GridLayout;
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
		panel.add(yDownButton);
		panel.add(yUpButton);
		panel.add(zDownButton);
		panel.add(zUpButton);
		panel.add(stackButton);
		panel.add(laserButton);

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

}
