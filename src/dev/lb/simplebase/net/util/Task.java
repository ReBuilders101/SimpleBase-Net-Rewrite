package dev.lb.simplebase.net.util;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.lb.simplebase.net.connection.NetworkConnection;

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
	 * Waits until the action associated with this task has completed.
	 * <p>
	 * The method will block the calling thread until the action completes or
	 * the waiting thread is interrupted.
	 * @return {@code true} if the action completed, {@code false} if the thread was interrupted while waiting
	 * @see #await(long, TimeUnit)
	 * @see #tryAwait()
	 */
	public default boolean await() {
		try {
			tryAwait();
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	/**
	 * Waits until the action associated with this task has completed or until a specified waiting time elapses.
	 * <p>
	 * The method will block the calling thread until the action completes or
	 * the waiting thread is interrupted or the timeout elapses.
	 * @param timeout The maximum amount of time to wait until this method returns, in the specified time unit
	 * @param unit The unit of time for the timeout value
	 * @return {@code true} if the action completed, {@code false} if the thread was interrupted while waiting or timed out
	 * @see #await()
	 * @see #tryAwait(long, TimeUnit)
	 */
	public default boolean await(long timeout, TimeUnit unit) {
		try {
			tryAwait(timeout, unit);
			return true;
		} catch (InterruptedException | TimeoutException e) {
			return false;
		}
	}
	
	/**
	 * Waits until the action associated with this task has completed.
	 * <p>
	 * The method will block the calling thread until the action completes or
	 * the waiting thread is interrupted.
	 * @throws InterruptedException When the thread was interrupted while waiting
	 * @see #await()
	 * @see #tryAwait(long, TimeUnit)
	 */
	public void tryAwait() throws InterruptedException;
	
	/**
	 * Waits until the action associated with this task has completed or until a specified waiting time elapses.
	 * <p>
	 * The method will block the calling thread until the action completes or
	 * the waiting thread is interrupted or the timeout elapses.
	 * @param timeout The maximum amount of time to wait until this method returns, in the specified time unit
	 * @param unit The unit of time for the timeout value
	 * @throws InterruptedException When the thread was interrupted while waiting
	 * @throws TimeoutException When the specified time elapsed without the task completing
	 * @see #await(long, TimeUnit)
	 * @see #tryAwait()
	 */
	public void tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;
	
	/**
	 * Adds a {@link Runnable} that will be run when the action associated with this task completes.<br>
	 * If this task is already done, the {@code Runnable} will run immediately.
	 * <p>
	 * This method makes no guarantees on what thread the supplied {@code Runnable} will be executed.
	 * Implementation may run the {@code Runnable} on the calling thread if this task is already done,
	 * on the thread that marks this task as done, or on any other thread.
	 * @param chainTask The {@link Runnable} to run when this task completes
	 * @return This task
	 * @see #thenAsync(Runnable)
	 */
	public Task then(Runnable chainTask);
	
	/**
	 * Adds a {@link Runnable} that will be run when the action associated with this task completes.<br>
	 * If this task is already done, the {@code Runnable} will run immediately.
	 * <p>
	 * Unlike {@link #then(Runnable)}, this method makes stronger guarantees on what thread the supplied
	 * {@code Runnable} will be executed: It will always use the {@link ForkJoinPool#commonPool()} to
	 * run, even if this task is already done.
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
}
