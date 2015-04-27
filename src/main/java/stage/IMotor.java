package stage;

public interface IMotor {

	public int Y_AXIS = 0;
	public int Z_AXIS = 1;

	public double POS_MIN_Y = 0;
	public double POS_MIN_Z = 0;
	public double POS_MAX_Y = 15;
	public double POS_MAX_Z = 15;

	public double VEL_MIN_Y = 0;
	public double VEL_MIN_Z = 0;
	public double VEL_MAX_Y = 1.5;
	public double VEL_MAX_Z = 1.5;

	public double getPosition(int axis) throws MotorException;

	public double getVelocity(int axis) throws MotorException;

	public boolean isMoving() throws MotorException;

	public boolean isMoving(int axis) throws MotorException;

	public void setVelocity(int axis, double vel) throws MotorException;

	public void setTarget(int axis, double pos) throws MotorException;

	public void stop() throws MotorException;

	public void close() throws MotorException;
}
