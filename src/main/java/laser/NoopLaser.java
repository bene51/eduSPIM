package laser;

public class NoopLaser implements ILaser {

	public static final double MAX_POWER = 200;

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

	@Override
	public double getMaxPower() {
		return MAX_POWER;
	}
}
