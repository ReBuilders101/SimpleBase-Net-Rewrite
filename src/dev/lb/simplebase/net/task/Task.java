package dev.lb.simplebase.net.task;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

import dev.lb.simplebase.net.GlobalTimer;
import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.util.Pair;

/**
 * A task represents an action that may execute asynchrounously.
 * <p>
 * Methods that return a {@link Task} typically cannot guaratee that the action will be completed
 * when the method returns (e.g. {@link NetworkConnection#openConnection()}). The {@link Task} object can
 * be used to monitor progress on the action and react when the action is completed.
 * <p>
 * While similar to the {@link Future} interface, a {@link Task} has no concept of success or failure and cannot return
 * a result. This is done to keep the interface as simple and as flexible as possible.<br>
 * If it is necessary to react based on the outcome if the executed action (e.g. the {@code openConnection()} method
 * can either successfully open or fail and close the connection), the state can usually be obtained directly from the
 * object that returned this task (in case of the {@code openConnection() example, use NetworkConnection#getCurrentState()}).
 */
public interface Task {

	/**
	 * {@code True} if the action associated with this task has been completed, {@code false} if
	 * it is still ongoing.
	 * @return {@code true} if this task is done, {@code false} otherwise
	 */
	public boolean isDone();
	
	/**
	 * {@code False} if the action associated with this task has been completed, {@code true} if
	 * it is still ongoing.
	 * @return {@code false} if this task is done, {@code true} otherwise
	 */
	public default boolean isRunning() {
		return !isDone();
	}
	
	/**
	 * Waits until the action associated with this task has completed.
	 * <p>
	 * The method will block the calling thread until the action completes or
	 * the waiting thread is interrupted.
	 * @return {@code this}
	 * @see #await(long, TimeUnit)
	 * @see #tryAwait()
	 */
	public default Task await() {
		try {
			return tryAwait();
		} catch (InterruptedException e) {
			return this;
		}
	}
	
	/**
	 * Waits until the action associated with this task has completed or until a specified waiting time elapses.
	 * <p>
	 * The method will block the calling thread until the action completes or
	 * the waiting thread is interrupted or the timeout elapses.
	 * @param timeout The maximum amount of time to wait until this method returns, in the specified time unit
	 * @param unit The unit of time for the timeout value
	 * @return {@code this}
	 * @see #await()
	 * @see #tryAwait(long, TimeUnit)
	 */
	public default Task await(long timeout, TimeUnit unit) {
		try {
			return tryAwait(timeout, unit);
		} catch (InterruptedException | TimeoutException e) {
			return this;
		}
	}
	
	/**
	 * Waits until the action associated with this task has completed.
	 * <p>
	 * The method will block the calling thread until the action completes or
	 * the waiting thread is interrupted.
	 * @return {@code this}
	 * @throws InterruptedException When the thread was interrupted while waiting
	 * @see #await()
	 * @see #tryAwait(long, TimeUnit)
	 */
	public Task tryAwait() throws InterruptedException;
	
	/**
	 * Waits until the action associated with this task has completed or until a specified waiting time elapses.
	 * <p>
	 * The method will block the calling thread until the action completes or
	 * the waiting thread is interrupted or the timeout elapses.
	 * @param timeout The maximum amount of time to wait until this method returns, in the specified time unit
	 * @param unit The unit of time for the timeout value
	 * @return {@code this}
	 * @throws InterruptedException When the thread was interrupted while waiting
	 * @throws TimeoutException When the specified time elapsed without the task completing
	 * @see #await(long, TimeUnit)
	 * @see #tryAwait()
	 */
	public Task tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;
	
	/**
	 * A non-blocking way to check whether a task has timed out
	 * @param timeout The amount of time that must have passed for this method to succeed in the specified time unit
	 * @param unit The unit of time for the timeout value
	 * @return {@code true} if the specified amount of time has passed since the task was created <b>OR the task has completed</b>,
	 *  {@code false} if the task is still running and the timespan has not passed.
	 */
	public boolean asyncAwait(long timeout, TimeUnit unit);
	
	/**
	 * Adds a {@link Runnable} that will be run when the action associated with this task completes.<br>
	 * If this task is already done, the {@code Runnable} will run immediately.
	 * <p>
	 * The {@code Runnable} will run on the calling thread if this task is already done,
	 * on the thread that marks this task as done. In that case, the {@code Runnable} will run completely
	 * before the waiting threads are released.
	 * @param chainTask The {@link Runnable} to run when this task completes
	 * @return This task
	 * @see #thenAsync(Runnable)
	 */
	public Task then(Runnable chainTask);
	
	/**
	 * Adds a {@link Runnable} that will be run when the action associated with this task completes.<br>
	 * If this task is already done, the {@code Runnable} will run immediately.
	 * <p>
	 * The supplied {@code Runnable} will always use the {@link ForkJoinPool#commonPool()} to
	 * run, even if this task is already done. The {@code Runnable} will be added to the thread pool
	 * before the waiting threads are released, but might execute at any later time.
	 * @param chainTask The {@link Runnable} to run when this task completes
	 * @return This task
	 * @see #then(Runnable)
	 */
	public Task thenAsync(Runnable chainTask);
	
	/**
	 * A {@link Task} that is already done.
	 * <p>
	 * Can be used in places where an action is executed synchronously, but the
	 * method is required to return a {@link Task}.
	 * @return A completed {@link Task}
	 */
	public static Task completed() {
		return CompletedTask.INSTANCE;
	}
	
	/**
	 * Creates a new task that will complete once all tasks in the parameters are completed
	 * @param tasks The tasks that are required to be completed
	 * @return The new combined task
	 */
	public static Task requireAll(Task...tasks) {
		return new CombinedTask(tasks);
	}
	
	/**
	 * Creates a {@link Task} and a completion function for this task.
	 * <p>
	 * The returned task will complete once the returned {@link Runnable} is called.
	 * </p>
	 * @return A {@link Task} and a {@link Runnable} that completes the task
	 */
	public static Pair<Task, Runnable> completable() {
		final AwaitableTask task = new AwaitableTask();
		return new Pair<>(task, task::release);
	}
	
	/**
	 * Creates a {@link Task} that automatically completes after a certain time
	 * @param time The timeout to wait
	 * @param unit The {@link TimeUnit} of that timeout
	 * @return A {@link Task} that completes after that time
	 */
	public static Task timeout(long time, TimeUnit unit) {
		final Pair<Task, Runnable> tac = Task.completable();
		GlobalTimer.delay(tac.getRight(), time, unit);
		return tac.getLeft();
	}
	
	/**
	 * Whether the task was completed at creation time
	 * @param task The task to test
	 * @return {@code true} if the task was created as complete and has never run, {@code false} otherwise
	 */
	public static boolean wasInitiallyCompleted(Task task) {
		return task instanceof CompletedTask;
	}
	
	/**
	 * Creates a {@link Task} that waits for the result of a {@link BooleanSupplier} to change to {@code true}.
	 * @param condition The condition to check
	 * @deprecated The await methods use a loop that constantly rechecks the condition, which
	 * prevents the waiting thread from being parked while waiting.
	 * @return A task that waits until the condition becomes {@code true}
	 */
	@Deprecated
	public static Task awaitCondition(BooleanSupplier condition) {
		return new Task() {
			
			private long startTimestamp = NetworkManager.getClockMillis();
			private volatile boolean doneOnce = false;
			
			@Override
			public Task tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
				final long msTimeout = unit.toMillis(timeout);
				final long endTimestamp = startTimestamp + msTimeout;
				
				while(NetworkManager.getClockMillis() < endTimestamp) {
					if(Thread.interrupted()) {
						throw new InterruptedException();
					}
					
					if(condition.getAsBoolean()) {
						doneOnce = true;
						return this;
					}
				}
				
				throw new TimeoutException();
			}
			
			@Override
			public Task tryAwait() throws InterruptedException {
				while(true) {
					if(Thread.interrupted()) {
						throw new InterruptedException();
					}
					
					if(condition.getAsBoolean()) {
						doneOnce = true;
						return this;
					}
				}
			}
			
			@Override
			public Task thenAsync(Runnable chainTask) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public Task then(Runnable chainTask) {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public boolean isDone() {
				return doneOnce || condition.getAsBoolean();
			}
			
			@Override
			public boolean asyncAwait(long timeout, TimeUnit unit) {
				final long msTimeout = unit.toMillis(timeout);
				final long endTimestamp = startTimestamp + msTimeout;
				return isDone() || NetworkManager.getClockMillis() >= endTimestamp;
			}
		};
	}
}
