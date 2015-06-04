package buttons;

import java.util.Arrays;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import main.ExceptionHandler;
import main.Microscope;

public class ArduinoButtons extends AbstractButtons implements SerialPortEventListener {

	private final SerialPort port;

	private int value = 0;
	private final Microscope microscope;

	private static final int N_BUTTONS = 7;

	public ArduinoButtons(String portname, Microscope microscope) throws ButtonsException {
		this.port = new SerialPort(portname);
		this.microscope = microscope;
		try {
			init();
		} catch(SerialPortException e) {
			throw new ButtonsException("Error initializing serial communication with the Arduino", e);
		}
	}

	private void init() throws SerialPortException {
		port.openPort();
		port.setParams(SerialPort.BAUDRATE_9600,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);
		port.addEventListener(this, SerialPort.MASK_RXCHAR);
		System.out.println("Arduino fine");
	}

	@Override
	public void serialEvent(SerialPortEvent serialPortEvent) {
		try {
			byte[] read = null;
			if(serialPortEvent.getEventValue() > 0)
				read = port.readBytes();

			if(read != null) {
				System.out.println(Arrays.toString(read));
				int newValue = read[0];
				int nButtonsPressed = Integer.bitCount(newValue);
				// either no button was pressed before, and now exactly one button is pressed:
				if(value == 0 && nButtonsPressed == 1) {
					int pressedButton = getButton(newValue);
					fireButtonPressed(pressedButton);
					value = newValue;
				}
				// or a button was pressed before, and now no button is pressed:
				else if(value != 0 && nButtonsPressed == 0) {
					fireButtonReleased(getButtonDown());
					value = 0;
				}
				// or we ignore it simply
			}
		} catch (SerialPortException e) {
			ExceptionHandler.handleException("Error communicating with the Arduino", e);
			try {
				close();
				init();
			} catch(Throwable t) {
				ExceptionHandler.handleException("Error after re-initializing communication with Arduino, exiting", t);
				microscope.shutdown(Microscope.EXIT_BUTTON_ERROR);
			}
		}
	}

	@Override
	public void close() throws ButtonsException {
		try {
			port.closePort();
		} catch (SerialPortException e) {
			throw new ButtonsException("Error closing serial port for arduino", e);
		}
	}

	private static int getButton(int value) {
		for(int i = 0; i < N_BUTTONS; i++) {
			if((value & (1 << i)) != 0) {
				return i;
			}
		}
		return -1;
	}

	public static void main(String[] args) throws ButtonsException {
		new ArduinoButtons("COM3", null);
	}
}
