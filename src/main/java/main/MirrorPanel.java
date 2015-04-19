package main;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class MirrorPanel extends Panel {

	private ArrayList<MirrorPanelListener> listeners = new ArrayList<MirrorPanelListener>();

	private final NumberField tf;

	public MirrorPanel() {
		setForeground(Color.WHITE);
		setBackground(Color.BLACK);

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);

		c.insets = new Insets(2, 4, 2, 4);
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		add(new Label("Mirror"), c);
		tf = new NumberField(8);
		tf.setText("" + Preferences.getStackYStart());
		tf.setForeground(Color.BLACK);
		c.gridx++;
		add(tf, c);
	}

	public double getPosition() {
		return Double.parseDouble(tf.getText());
	}

	public void addMirrorPanelListener(MirrorPanelListener l) {
		listeners.add(l);
	}

	public void removeMirrorPanelListener(MirrorPanelListener l) {
		listeners.remove(l);
	}

	private void fireEvent(KeyEvent e) {
		for(MirrorPanelListener l : listeners)
			l.keyPressed(e);
	}

	private final class NumberField extends TextField {
		public NumberField(int n) {
			super(n);
			addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if(e.getKeyCode() == KeyEvent.VK_UP) {
						StringBuffer text = new StringBuffer(getText());
						int car = getCaretPosition();
						int originalCar = car;
						if(car == text.length())
							car--;

						for(; car >= 0; car--) {
							char ch = text.charAt(car);
							if(ch == '.')
								continue;
							int digit = Integer.parseInt(Character.toString(ch));
							if(digit < 9) {
								text.setCharAt(car, Integer.toString(digit + 1).charAt(0));
								setText(text.toString());
								break;
							}
							text.setCharAt(car, '0');
							if(car == 0) {
								text.insert(0, '1');
								setText(text.toString());
								setCaretPosition(originalCar + 1);
							}
						}
						e.consume();
					} // VK_UP
					else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
						StringBuffer text = new StringBuffer(getText());
						int car = getCaretPosition();
						int originalCar = car;
						if(car == text.length())
							car--;

						for(; car >= 0; car--) {
							char ch = text.charAt(car);
							if(ch == '.')
								continue;
							int digit = Integer.parseInt(Character.toString(ch));
							if(digit > 0) {
								if(car == 0 && digit == 1 && text.charAt(1) != '.') {
									text.deleteCharAt(0);
									setCaretPosition(Math.max(0, originalCar - 1));
								} else {
									text.setCharAt(car, Integer.toString(digit - 1).charAt(0));
								}
								setText(text.toString());
								break;
							}
							text.setCharAt(car, '9');
							if(car == 0 && text.charAt(1) != '.') {
								text.deleteCharAt(0);
								setText(text.toString());
								setCaretPosition(Math.max(0, originalCar - 1));
							}
						}
						e.consume();
					} // VK_DOWN
					fireEvent(e);
				}
			});
		}
	}

	public static void main(String... args) {
		Frame f = new Frame();
		f.add(new MirrorPanel());
		f.pack();
		f.setVisible(true);
	}
}
