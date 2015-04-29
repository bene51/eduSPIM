package main;


public class SingleElementThreadQueue {
		private Thread thread;
		private final Object lock = new Object();
		private boolean shutdown = false;

		private Runnable lastEvent = null;

		private Runnable event = null;

		private boolean idle = true;

		public SingleElementThreadQueue() {
			thread = new Thread() {
				@Override
				public void run() {
					try {
						idle = false;
						loop();
					} catch(Throwable e) {
						ExceptionHandler.handleException("Unexpected exception in SingleElementThreadQueue", e);
					}
				}
			};
			thread.start();
		}

		public boolean isIdle() {
			return idle;
		}

		public void loop() {
			while(!shutdown) {
				Runnable e = poll();
				// happens if shutdown
				if(e == null)
					return;
				if(!e.equals(lastEvent))
					e.run();
			}
		}

		/**
		 * Not the same <code>Runnable</code> object may be re-used!
		 * @param e
		 */
		public void push(Runnable e) {
			synchronized(lock) {
				event = e;
			}
			synchronized(this) {
				notifyAll();
			}
		}

		public Runnable poll() {
			if(event == null) {
				synchronized(this) {
					try {
						idle = true;
						wait();
						idle = false;
					} catch (InterruptedException e) {
						ExceptionHandler.handleException("Interrupted while waiting for an event in the SingleElementThreadQueue", e);
					}
				}
			}
			Runnable ret = null;
			synchronized(lock) {
				ret = event;
				event = null;
			}
			return ret;
		}

		public void shutdown() {
			shutdown = true;
			synchronized(this) {
				notifyAll();
			}
			try {
				thread.join();
			} catch (InterruptedException e) {
				ExceptionHandler.handleException("Interrupted while joining the execution thread in SingleElementThreadQueue", e);
			}
		}
	}