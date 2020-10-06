package dev.lb.simplebase.net;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.StaticType;
import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.task.Task;
import dev.lb.simplebase.net.util.InternalAccess;
import dev.lb.simplebase.net.util.Lazy;
import dev.lb.simplebase.net.util.Pair;

/**
 * Handles the global connection state check.<p>
 * Any {@link NetworkManagerCommon} will be registered here if {@link CommonConfig#getGlobalConnectionCheck()} is set to {@code true}
 * when it is created. An internal daemon thread will periodically check whether any connections have timed out, an the connection will
 * automatically close if that is the case.
 */
@StaticType
public class GlobalTimer {
	//Timer starts the thread in the constructor. Use Lazy so no thread is started if a program doesn't use the GlobalTimer
	private static final Lazy<Timer> timer = Lazy.of(() -> new Timer("SimpleBase-Net-GlobalTimer"));
	private static volatile ManagerTimerTask currentTask = null;
	private static volatile long currentTaskPeriod = TimeUnit.SECONDS.toMillis(60);
	private static final Object taskPeriodLock = new Object();
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This class is used internally by the API and the contained methods should not and can not be called directly.
	 * </p><hr><p>
	 * Subscribes a manager to the global connection check mechanism.
	 * This will regularly call {@link NetworkManagerCommon#updateConnectionStatus()} on the supplied manager.
	 * </p><p>
	 * Managers will be automatically subscribed on creation and unsubscribed on cleanup when
	 * {@link CommonConfig#getGlobalConnectionCheck()} is enabled.
	 * </p>
	 * @param manager The manager to subscribe
	 */
	@Internal
	public static void subscribeManagerForConnectionStatusCheck(NetworkManagerCommon manager) {
		InternalAccess.assertCaller(NetworkManagerCommon.class, 0, "Cannot call subscribeManagerForConnectionStatusCheck() directly");
		GlobalTimer.assertTimerTask();
		ManagerTimerTask.register(manager);
	}
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This class is used internally by the API and the contained methods should not and can not be called directly.
	 * </p><hr><p>
	 * Unsubscribes a manager from the global connection check mechanism.
	 * This will stop regularly calling {@link NetworkManagerCommon#updateConnectionStatus()} on the supplied manager.
	 * </p><p>
	 * Managers will be automatically subscribed on creation and unsubscribed on cleanup when
	 * {@link CommonConfig#getGlobalConnectionCheck()} is enabled.
	 * </p>
	 * @param manager The manager to unsubscribe
	 */
	@Internal
	public static void unsubscribeManagerForConnectionStatusCheck(NetworkManagerCommon manager) {
		InternalAccess.assertCaller(NetworkManagerCommon.class, 0, "Cannot call unsubscribeManagerForConnectionStatusCheck() directly");
		ManagerTimerTask.unregister(manager);
	}

	private static void assertTimerTask() {
		synchronized (taskPeriodLock) {
			if(currentTask == null) {
				currentTask = ManagerTimerTask.createNext();
				timer.get().schedule(currentTask, 0, currentTaskPeriod);
			}
		}
	}
	
	/**
	 * The only method that syncs, just to avoid parallel calls
	 * @param millis The new period in milliseconds
	 */
	@Internal
	protected static void setManagerTimeout(long millis) {
		synchronized (taskPeriodLock) {
			if(currentTask == null) {
				currentTaskPeriod = millis;
			} else if(currentTaskPeriod != millis) {
				currentTaskPeriod = millis;
				currentTask.cancel();
				currentTask = null;
				
			} else { //Times are equal, nothing to do
				return;
			}
			assertTimerTask();
		}
	}
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This class is used internally by the API and the contained methods should not and can not be called directly.
	 * </p><hr><p>
	 * Cretates a task that completes after the given amount of time.
	 * </p><p>
	 * Use {@link Task#timeout(long, TimeUnit)} instead.
	 * </p>
	 * @param milliseconds The timeout in milliseconds
	 * @return The requested {@link Task}
	 */
	@Internal
	public static Task subscribeTimeoutTaskOnce(long milliseconds) {
		InternalAccess.assertCaller(Task.class, 0, "Cannot call subscribeTimeoutTaskOnce() directly");
		
		Pair<Task, Runnable> task = Task.completable();
		timer.get().schedule(new FunctionalTimerTask(task.getRight()), milliseconds);
		return task.getLeft();
	}
	
	/**
	 * Implementation of {@link TimerTask} that wraps a {@link Runnable}.
	 */
	private static final class FunctionalTimerTask extends TimerTask {
		private final Runnable runnable;
		private FunctionalTimerTask(Runnable runnable) {
			this.runnable = Objects.requireNonNull(runnable, "Cannot run null as a TimerTask");
		}
		@Override
		public void run() {
			runnable.run();
		}
	}

	/**
	 * Implementation of {@link TimerTask} that ticks the static manager list.<br>
	 * Only one scheduled instance can exist at a time.
	 */
	private static final class ManagerTimerTask extends TimerTask {
		private static final AtomicBoolean singleton = InternalAccess.createSingleton();
		private static final Set<NetworkManagerCommon> tickingManagers = new HashSet<>();
		private static final StampedLock fastRWLock = new StampedLock();
		
		private ManagerTimerTask() {
			InternalAccess.assertSingleton(singleton, "Cannot create new ManagerTimerTask before the old one is cancelled");
		}
		
		@Override
		public void run() {
			final long stamp = fastRWLock.readLock();
			try {
				for(NetworkManagerCommon nmc : tickingManagers) {
					nmc.updateConnectionStatus();
				}
			} finally {
				fastRWLock.unlockRead(stamp);
			}
		}
		
		@Override
		public boolean cancel() {
			final boolean result = super.cancel();
			InternalAccess.freeSingleton(singleton, "No ManagerTimerTask to cancel?????");
			return result;
		}
		
		public static void register(NetworkManagerCommon nmc) {
			final long stamp = fastRWLock.writeLock();
			try {
				tickingManagers.add(nmc);
			} finally {
				fastRWLock.unlockWrite(stamp);
			}
		}
		
		public static void unregister(NetworkManagerCommon nmc) {
			final long stamp = fastRWLock.writeLock();
			try {
				tickingManagers.remove(nmc);
			} finally {
				fastRWLock.unlockWrite(stamp);
			}
		}
		
		public static ManagerTimerTask createNext() {
			return new ManagerTimerTask();
		}
	}
}
