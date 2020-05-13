package dev.lb.simplebase.net;

import java.util.function.Consumer;
import java.util.function.Function;

import dev.lb.simplebase.net.annotation.Threadsafe;

/**
 * Provides a way to execute an external action for an object while it is in a synchronized state, to prevent
 * concurrent modification while the action is running.
 * @param <T>
 */
public interface ThreadsafeAction<T extends ThreadsafeAction<T>> {
	
	/**
	 * Executes an action for this object while holding a lock/monitor that pervents
	 * concurrent modification while the action is running.
	 * The exact type of lock or monitor object depends on the implementation.
	 * As any other operation waiting for a lock/monitor, calling this method carelessly can lead to deadlocks.
	 * @param action The action to execute for this object
	 */
	@Threadsafe
	public void action(Consumer<T> action);
	
	/**
	 * Executes an action for this object while holding a lock/monitor that pervents
	 * concurrent modification while the action is running.
	 * The exact type of lock or monitor object depends on the implementation.
	 * As any other operation waiting for a lock/monitor, calling this method carelessly can lead to deadlocks.
	 * @param action The action to execute for this object that returns a value
	 * @return The result of the action
	 */
	@Threadsafe
	public <R> R actionReturn(Function<T, R> action);
	
}
