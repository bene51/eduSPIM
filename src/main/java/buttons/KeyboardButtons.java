package buttons;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


public class KeyboardButtons extends AbstractButtons implements KeyListener {

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {
		int kc = e.getKeyCode();

		if(super.getButtonDown() != -1) {
			if(kc == KeyEvent.VK_ESCAPE)
				super.fireButtonReleased(super.getButtonDown());
			return;
		}

		switch(kc) {
		case KeyEvent.VK_UP:    super.fireButtonPressed(BUTTON_Y_UP);   break;
		case KeyEvent.VK_DOWN:  super.fireButtonPressed(BUTTON_Y_DOWN); break;
		case KeyEvent.VK_RIGHT: super.fireButtonPressed(BUTTON_Z_DOWN); break;
		case KeyEvent.VK_LEFT:  super.fireButtonPressed(BUTTON_Z_UP);   break;
		case KeyEvent.VK_S:     super.fireButtonPressed(BUTTON_STACK);  break;
		case KeyEvent.VK_L:     super.fireButtonPressed(BUTTON_LASER);  break;
		case KeyEvent.VK_I:     super.fireButtonPressed(BUTTON_INFO);   break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
//		int down = super.getButtonDown();
//		if(down == -1)
//			return;
//
//		int kc = e.getKeyCode();
//		switch(kc) {
//		case KeyEvent.VK_UP:    if(down == BUTTON_Y_UP)   super.fireButtonReleased(down); break;
//		case KeyEvent.VK_DOWN:  if(down == BUTTON_Y_DOWN) super.fireButtonReleased(down); break;
//		case KeyEvent.VK_RIGHT: if(down == BUTTON_Z_DOWN) super.fireButtonReleased(down); break;
//		case KeyEvent.VK_LEFT:  if(down == BUTTON_Z_UP)   super.fireButtonReleased(down); break;
//		case KeyEvent.VK_S:     if(down == BUTTON_STACK)  super.fireButtonReleased(down); break;
//		case KeyEvent.VK_L:     if(down == BUTTON_LASER)  super.fireButtonReleased(down); break;
//		case KeyEvent.VK_I:     if(down == BUTTON_INFO)   super.fireButtonReleased(down); break;
//		}
	}

	@Override
	public void close() throws ButtonsException {}
}
