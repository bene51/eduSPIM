package serial;

public class ListSerialPorts {

	public static class Port {
		public final String port;
		public final String name;

		public Port(String port, String name) {
			this.port = port;
			this.name = name;
		}

		@Override
		public String toString() {
			return port + ": " + name;
		}
	}

	static {
		System.loadLibrary("enumerate_serial");
	}

	private static native String[] enumeratePorts();

	public static Port[] enumPorts() {
		String[] ret = enumeratePorts();

		String[] friendlyNames = ret[0].split("\n");
		String[] portNames = ret[1].split("\n");

		int n = friendlyNames.length;
		Port[] ports = new Port[n];
		for(int i = 0; i < n; i++)
			ports[i] = new Port(portNames[i], friendlyNames[i]);

		return ports;
	}

	public static void main(String... args) {
		Port[] ports = enumPorts();
		for(Port p : ports)
			System.out.println(p);
	}
}
