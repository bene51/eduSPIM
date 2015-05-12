package laser;

public interface ILaser {

	public void close() throws LaserException;

	public void setPower(double power) throws LaserException;

	public void setOn() throws LaserException;

	public void setTriggered() throws LaserException;

	public void setOff() throws LaserException;

	public double getMaxPower();
}
