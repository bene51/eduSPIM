package laser;

public class NoopLaser implements ILaser {

	@Override
	public void close() {}

	@Override
	public void setPower(double power) {}

	@Override
	public void setOn() {}

	@Override
	public void setTriggered() {}

	@Override
	public void setOff() {}
}
