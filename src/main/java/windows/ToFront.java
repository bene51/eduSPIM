package windows;

public class ToFront {

	static {
		System.loadLibrary("to_front");
	}

	public static native void toFront();
}
