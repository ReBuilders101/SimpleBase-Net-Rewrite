package dev.lb.simplebase.net;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.StaticType;
import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;

/**
 * Handles the global connection state check.<p>
 * Any {@link NetworkManagerCommon} will be registered here if {@link CommonConfig#getGlobalConnectionCheck()} is set to {@code true}
 * when it is created. An internal daemon thread will periodically check whether any connections have timed out, an the connection will
 * automatically close if that is the case.
 */
@StaticType
public class GlobalConnectionCheck {
	private static final long DEFAULT_SLEEP_TIME = 30000;
	
	protected static void subscribe(NetworkManagerCommon manager) {
		if(!thread.isAlive()) {
			thread.start();
		}
		synchronized (managers) {
			managers.add(manager);
		}
	}
	
	protected static void unsubscribe(NetworkManagerCommon manager) {
		synchronized (managers) {
			managers.remove(manager);
		}
	}
	
	
	private static volatile long nextSleepTime = DEFAULT_SLEEP_TIME;
	private static final Set<NetworkManagerCommon> managers = new HashSet<>();
	
	
	/**
	 * Sets the timeout between the checks.
	 * @param millisconds The number of milliseconds that the thread should sleep for between checks
	 */
	public void setTimeout(long millisconds) {
		nextSleepTime = millisconds;
	}
	
	/**
	 * Sets the timeout between the checks.
	 * @param value The amount of time (in the given unit) that the thread shoul sleep for between checks
	 * @param unit The {@link TimeUnit} for the time amount. Will be converted to milliseconds internally.
	 */
	public void setTimeout(long value, TimeUnit unit) {
		setTimeout(unit.toMillis(value));
	}
	
	/**
	 * Immediately checks connections state as if the timeout had elapsed normally, and resets the
	 * timeout back to the set value.
	 */
	public void checkNow() {
		thread.interrupt();
	}
	
	private static final CheckTaskThread thread = new CheckTaskThread();
	
	@Internal
	private static class CheckTaskThread extends Thread {
		private static AtomicBoolean created = new AtomicBoolean(false);
		
		private CheckTaskThread() {
			if(!created.compareAndSet(false, true)) throw new RuntimeException("Can only create one instance if CheckTaskThread");
			setDaemon(true);
			setName("GlobalConnectionCheckThread");
		}

		@Override
		public void run() {
			while(true) {
				try {
					Thread.sleep(nextSleepTime);
				} catch (InterruptedException e) {
					//This is perfectly fine
				}
				//Do the check
				synchronized (managers) {
					managers.forEach(NetworkManagerCommon::updateConnectionStatus); 
				}
			}
		}
	}
}
