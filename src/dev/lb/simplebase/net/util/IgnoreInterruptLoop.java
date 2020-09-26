package dev.lb.simplebase.net.util;

@FunctionalInterface
public interface IgnoreInterruptLoop {

	public void run() throws InterruptedException;
	
	
	public static void loop(IgnoreInterruptLoop loop) {
		boolean done = false;
		while(!done) {
			try {
				loop.run();
				done = true;
			} catch (InterruptedException e) {}
		}
	}
}
