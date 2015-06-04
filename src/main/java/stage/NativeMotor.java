package stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NativeMotor implements IMotor {

	public static final String STAGE_Y = "M-112.2DG-NEW";
	public static final String STAGE_Z = "M-111.1DG-NEW";
	public static final String STAGE_M = "N-470K021";

	private static final String[] STAGES = new String[N_AXES];
	static {
		if(Y_AXIS < STAGES.length) STAGES[Y_AXIS] = STAGE_Y;
		if(Z_AXIS < STAGES.length) STAGES[Z_AXIS] = STAGE_Z;
		if(MIRROR < STAGES.length) STAGES[MIRROR] = STAGE_M;
	};

	private static final int BAUD_RATE = 115200;

	static {
		System.loadLibrary("stage_NativeMotor");
	}

	public NativeMotor(int comPort) throws MotorException {
		stageConnect(comPort, BAUD_RATE, STAGES);
		for(int axis = 0; axis < STAGES.length; axis++) {
			if(stageIsReferenceNeeded(axis)) {
				boolean allow = true;
				if(allow)
					stageReferenceIfNeeded(axis);
			}
		}
	}

	private static native void stageConnect(int comport, int baudrate, String[] stages) throws MotorException;
	private static native boolean stageIsReferenceNeeded(int axis) throws MotorException;
	private static native void stageReferenceIfNeeded(int axis) throws MotorException;
	private static native double stageGetPosition(int axis) throws MotorException;
	private static native double stageGetVelocity(int axis) throws MotorException;
	private static native boolean stageIsMoving(int axis) throws MotorException;
	private static native void stageSetVelocity(int axis, double vel) throws MotorException;
	private static native void stageSetTarget(int axis, double pos) throws MotorException;
	private static native void stageStopMoving(int axis) throws MotorException;
	private static native void stageClose() throws MotorException;

	@Override
	public double getPosition(int axis) throws MotorException {
		if(axis < N_AXES)
			return stageGetPosition(axis);
		return 0;
	}

	@Override
	public double getVelocity(int axis) throws MotorException {
		if(axis < N_AXES)
			return stageGetVelocity(axis);
		return 0;
	}

	@Override
	public boolean isMoving() throws MotorException {
		for(int axis = 0; axis < N_AXES; axis++) {
			if(isMoving(axis))
				return true;
		}
		return false;
	}

	@Override
	public boolean isMoving(int axis) throws MotorException {
		if(axis < N_AXES)
			return stageIsMoving(axis);
		return false;
	}

	@Override
	public void setVelocity(int axis, double vel) throws MotorException {
		if(axis < N_AXES)
			stageSetVelocity(axis, vel);
	}

	@Override
	public void setTarget(int axis, double pos) throws MotorException {
		if(axis < N_AXES)
			stageSetTarget(axis, pos);
	}

	@Override
	public void stop(int axis) throws MotorException {
		if(axis < N_AXES)
			stageStopMoving(axis);
	}

	@Override
	public void close() throws MotorException {
		stageClose();
	}

	public static void main(String[] args) throws NumberFormatException, IOException, MotorException {
		NativeMotor motor = new NativeMotor(5);
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
