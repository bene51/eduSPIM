package buttons;

import java.util.ArrayList;

public abstract class AbstractButtons {

	public static final int BUTTON_Y_DOWN = 0;
	public static final int BUTTON_Y_UP   = 1;
	public static final int BUTTON_Z_DOWN = 2;
	public static final int BUTTON_Z_UP   = 3;
	public static final int BUTTON_STACK  = 4;
	public static final int BUTTON_LASER  = 5;

	private int buttonDown = -1;

	private ArrayList<ButtonsListener> listeners = new ArrayList<ButtonsListener>();

	public void addButtonsListener(ButtonsListener l) {
		listeners.add(l);
	}

	public void removeButtonsListener(ButtonsListener l) {
		listeners.remove(l);
	}

	public int getButtonDown() {
		return buttonDown;
	}

	public void close() {}

	protected void fireButtonPressed(final int button) {
		System.out.println("button pressed: " + button);
		// any button is already down, don't fire additional events before it's released
		synchronized(this) {
			if(buttonDown != -1)
				return;
			buttonDown = button;
		}
		new Thread() {
			@Override
			public void run() {
				for(ButtonsListener l : listeners)
					l.buttonPressed(button);
			}
		}.start();
	}

	protected void fireButtonReleased(final int button) {
		System.out.println("button released: " + button);
		// This should not happen, let's just be sure
		synchronized(this) {
			if(button != buttonDown)
				System.out.println("Button " + button + " was released but button " + buttonDown + " was pressed before");
			buttonDown = -1;
		}
		new Thread() {
			@Override
			public void run() {
				for(ButtonsListener l : listeners)
					l.buttonReleased(button);
			}
		}.start();
	}
}
