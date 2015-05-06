package stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NativeMotor implements IMotor {

	private static final int BAUD_RATE = 38400;

	static {
		System.loadLibrary("stage_NativeMotor");
	}

	public NativeMotor(int comPort) throws MotorException {
		stageConnect(comPort, BAUD_RATE);
		if(stageIsReferenceNeeded()) {
			boolean allow = true; // TODO check with Wiebke the order of referencing
			if(allow)
				stageReferenceIfNeeded();
		}
	}

	private static native void stageConnect(int comport, int baudrate) throws MotorException;
	private static native boolean stageIsReferenceNeeded() throws MotorException;
	private static native void stageReferenceIfNeeded() throws MotorException;
	private static native double stageGetPosition(int axis) throws MotorException;
	private static native double stageGetVelocity(int axis) throws MotorException;
	private static native boolean stageIsMoving(int axis) throws MotorException;
	private static native void stageSetVelocity(int axis, double vel) throws MotorException;
	private static native void stageSetTarget(int axis, double pos) throws MotorException;
	private static native void stageStopMoving() throws MotorException;
	private static native void stageClose() throws MotorException;

	@Override
	public double getPosition(int axis) throws MotorException {
		return stageGetPosition(axis);
	}

	@Override
	public double getVelocity(int axis) throws MotorException {
		return stageGetVelocity(axis);
	}

	@Override
	public boolean isMoving() throws MotorException {
		return isMoving(IMotor.Y_AXIS) || isMoving(IMotor.Z_AXIS);
	}

	@Override
	public boolean isMoving(int axis) throws MotorException {
		return stageIsMoving(axis);
	}

	@Override
	public void setVelocity(int axis, double vel) throws MotorException {
		stageSetVelocity(axis, vel);
	}

	@Override
	public void setTarget(int axis, double pos) throws MotorException {
		stageSetTarget(axis, pos);
	}

	@Override
	public void stop() throws MotorException {
		stageStopMoving();
	}

	@Override
	public void close() throws MotorException {
		stageClose();
	}

	public static void main(String[] args) throws NumberFormatException, IOException, MotorException {
		NativeMotor motor = new NativeMotor(7);
		System.out.println("motor initialized");
		double y = motor.getPosition(Y_AXIS);
		double z = motor.getPosition(Z_AXIS);
		System.out.println("y = " + y + ", z = " + z);

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line;
		while (!(line = in.readLine()).equals("quit")) {
			String[] toks = line.split(" ");
			motor.setTarget(Y_AXIS, Double.parseDouble(toks[0]));
			motor.setTarget(Z_AXIS, Double.parseDouble(toks[1]));
			while(motor.isMoving()) {
				y = motor.getPosition(Y_AXIS);
				z = motor.getPosition(Z_AXIS);
				System.out.println("y = " + y + ", z = " + z);
			}
		}
		motor.close();
	}
}
