package main;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

@SuppressWarnings("serial")
public class NumberField extends JTextField {

	private ArrayList<KeyListener> listener = new ArrayList<KeyListener>();

	@Override
	public void addKeyListener(KeyListener l) {
		listener.add(l);
	}

	@Override
	public void removeKeyListener(KeyListener l) {
		listener.remove(l);
	}

	private void fireKeyPressed(KeyEvent e) {
		for(KeyListener l : listener)
			l.keyPressed(e);
	}

	private void fireKeyReleased(KeyEvent e) {
		for(KeyListener l : listener)
			l.keyReleased(e);
	}

	private void fireKeyTyped(KeyEvent e) {
		for(KeyListener l : listener)
			l.keyTyped(e);
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new NumberField(8));
		frame.pack();
		frame.setVisible(true);
	}

	public NumberField(int n) {
		super(n);
		InputMap im = getInputMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "bla");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "bla");
		super.addKeyListener(new KeyAdapter() {
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
							setCaretPosition(originalCar);
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
							int carP = 0;
							if(car == 0 && digit == 1 && text.charAt(1) != '.') {
								text.deleteCharAt(0);
								carP = Math.max(0, originalCar - 1);
							} else {
								text.setCharAt(car, Integer.toString(digit - 1).charAt(0));
								carP = originalCar;
							}
							setText(text.toString());
							setCaretPosition(carP);
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
				fireKeyPressed(e);
			} // keyPressed

			@Override
			public void keyReleased(KeyEvent e) {
				fireKeyReleased(e);
			}

			@Override
			public void keyTyped(KeyEvent e) {
				fireKeyTyped(e);
			}
		});
	}
}