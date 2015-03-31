package stage;

public class NativeMotor implements IMotor {

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

	public void close() {
		stageClose();
	}

	public static void main(String[] args) {

	}

}
