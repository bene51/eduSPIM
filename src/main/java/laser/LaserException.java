package laser;

@SuppressWarnings("serial")
public class LaserException extends Exception {

	public LaserException(String message, Exception e) {
		super(message, e);
	}
}
