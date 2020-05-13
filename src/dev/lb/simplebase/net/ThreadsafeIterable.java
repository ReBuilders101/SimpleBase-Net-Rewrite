package dev.lb.simplebase.net;

import java.util.ConcurrentModificationException;
import java.util.function.Consumer;
import dev.lb.simplebase.net.annotation.Threadsafe;

/**
 * Provides a way to iterate over an iterable that can be modified from another thread without risking
 * a {@link ConcurrentModificationException} by acquiring and holding a lock/monitor during the iteration.
 * @see ThreadsafeAction
 * @param <T> The type of the ThreadsafeIterable implementation class
 * @param <I> The type of the content elements to iterate over
 */
public interface ThreadsafeIterable<T extends ThreadsafeIterable<T, I>, I> extends Iterable<I>, ThreadsafeAction<T> {

	/**
	 * Iterates over this iterable while holding a lock/monitor to prevent concurrent modification.
	 * The exact type of lock or monitor object depends on the implementation.
	 * As any other operation waiting for a lock/monitor, calling this method carelessly can lead to deadlocks.
	 * @param iteratorHandler The threadsafe operation that should be executed for every item in the iterable
	 */
	@Threadsafe
	public void iterate(Consumer<I> itemAction);

}
