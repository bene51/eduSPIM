package stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NativeMotor implements IMotor {

	static {
		System.loadLibrary("stage_NativeMotor");
	}

	public NativeMotor(int comPort, int baudRate) {
		stageConnect(comPort, baudRate);
		if(stageIsReferenceNeeded()) {
			boolean allow = true; // TODO ask the user for permission to move and reference
			if(allow)
				stageReferenceIfNeeded();
		}
	}

	private static native void stageConnect(int comport, int baudrate);
	private static native boolean stageIsReferenceNeeded();
	private static native void stageReferenceIfNeeded();
	private static native double stageGetPosition(int axis);
	private static native double stageGetVelocity(int axis);
	private static native boolean stageIsMoving(int axis);
	private static native void stageSetVelocity(int axis, double vel);
	private static native void stageSetTarget(int axis, double pos);
	private static native void stageStopMoving();
	private static native void stageClose();

	public double getPosition(int axis) {
		return stageGetPosition(axis);
	}

	public double getVelocity(int axis) {
		return stageGetVelocity(axis);
	}

	public boolean isMoving() {
		return isMoving(IMotor.Y_AXIS) || isMoving(IMotor.Z_AXIS);
	}

	public boolean isMoving(int axis) {
		return stageIsMoving(axis);
	}

	public void setVelocity(int axis, double vel) {
		stageSetVelocity(axis, vel);
	}

	public void setTarget(int axis, double pos) {
		stageSetTarget(axis, pos);
	}

	public void stop() {
		stageStopMoving();
	}

	public void close() {
		stageClose();
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		NativeMotor motor = new NativeMotor(7, 38400);
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
