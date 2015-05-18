package stage;

public interface IMotor {

	public enum Axis {
		Y_AXIS,
		Z_AXIS,
	};

	public static final int N_AXES = Axis.values().length;
	public static final int Y_AXIS = Axis.Y_AXIS.ordinal();
	public static final int Z_AXIS = Axis.X_AXIS.ordinal();

	public static final double POS_MIN_Y = 0;
	public static final double POS_MIN_Z = 0;
	public static final double POS_MAX_Y = 15;
	public static final double POS_MAX_Z = 15;

	public static final double VEL_MIN_Y = 0;
	public static final double VEL_MIN_Z = 0;
	public static final double VEL_MAX_Y = 1.5;
	public static final double VEL_MAX_Z = 1.5;

	public double getPosition(int axis) throws MotorException;

	public double getVelocity(int axis) throws MotorException;

	public boolean isMoving() throws MotorException;

	public boolean isMoving(int axis) throws MotorException;

	public void setVelocity(int axis, double vel) throws MotorException;

	public void setTarget(int axis, double pos) throws MotorException;

	public void stop(int axis) throws MotorException;

	public void close() throws MotorException;
}
