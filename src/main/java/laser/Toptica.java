package laser;

import jssc.SerialPort;
import jssc.SerialPortException;

public class Toptica implements ILaser {

	private static final char TERM_CHAR = '\r';

	private final SerialPort port;

	public Toptica(String portname) throws LaserException {
		port = new SerialPort(portname);
		try {
			port.openPort();
			port.setParams(
					SerialPort.BAUDRATE_115200,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.FLOWCONTROL_NONE);
			port.writeString("la on"       + TERM_CHAR);
			port.writeString("ch 1 pow 0"  + TERM_CHAR);
			port.writeString("ch 2 pow 50" + TERM_CHAR);
			port.writeString("en 1"        + TERM_CHAR);
			port.writeString("en 2"        + TERM_CHAR);
			port.writeString("en ext"      + TERM_CHAR);
		} catch(Exception e) {
			throw new LaserException("Cannot initialize communication to laser ", e);
		}
	}

	@Override
	public void close() throws LaserException {
		if(port != null) {
			try {
				port.writeString("la off" + TERM_CHAR);
				port.closePort();
			} catch(SerialPortException e) {
				throw new LaserException("Error closing serial port for laser", e);
			}
		}
	}

	@Override
	public void setPower(double power) throws LaserException {
		try {
			port.writeString("ch 2 pow " + (int)Math.round(power) + TERM_CHAR);
		} catch (Exception e) {
			throw new LaserException("Error setting laser power", e);
		}
	}

	@Override
	public void setOn() throws LaserException {
		try {
			port.writeString("la on"   + TERM_CHAR);
			port.writeString("dis ext" + TERM_CHAR);
		} catch (Exception e) {
			throw new LaserException("Error switching laser to continuous mode", e);
		}
	}

	@Override
	public void setTriggered() throws LaserException {
		try {
			port.writeString("la on"   + TERM_CHAR);
			port.writeString("en ext"  + TERM_CHAR);
		} catch (Exception e) {
			throw new LaserException("Error switching laser to continuous mode", e);
		}
	}

	@Override
	public void setOff() throws LaserException {
		try {
			port.writeString("la off" + TERM_CHAR);
		} catch (Exception e) {
			throw new LaserException("Error switching laser off", e);
		}
	}
}
