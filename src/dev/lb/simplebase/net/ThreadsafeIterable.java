package dev.lb.simplebase.net;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Spliterator;
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
	public void iterate(Consumer<? super I> itemAction);

	/**
	 * Creates a new {@link Iterator} for this object only if the current thread already holds the lock/monitor of
	 * this object. Otherwise, it throws an Exception.
	 * @return A new {@link Iterator} for this object
	 * @throws IllegalStateException If the current thread does not already own the lock/monitor
	 */
	public Iterator<I> threadsafeIterator();
	
	/**
	 * Creates a new {@link Spliterator} for this object only if the current thread already holds the lock/monitor of
	 * this object. Otherwise, it throws an Exception.
	 * @return A new {@link Spliterator} for this object
	 * @throws IllegalStateException If the current thread does not already own the lock/monitor
	 */
	public Spliterator<I> threadsafeSpliterator();
	
	/**
	 * The iterator provides unsynchronized access to the iterable.
	 * To ensure thread safety, use {@link #action(Consumer)} to acquire
	 * the lock/monitor for this object and then use {@link #threadsafeIterator()}
	 * inside the action to use a synchronized iterator.
	 * @return An unsynchronized iterator, <b>use discouraged</b>
	 */
	@Deprecated
	@Override
	public Iterator<I> iterator();

	/**
	 * It is not recommended to use this method for {@link ThreadsafeIterable}s, because
	 * safety while iterating is not guaranteed.<p><b>Use {@link #iterate(Consumer)} instead</b></p>
	 */
	@Deprecated
	@Override
	public default void forEach(Consumer<? super I> consumer) {
		Iterable.super.forEach(consumer);
	}

	/**
	 * The spliterator provides unsynchronized access to the iterable.
	 * To ensure thread safety, use {@link #action(Consumer)} to acquire
	 * the lock/monitor for this object and then use {@link #threadsafeSpliterator()}
	 * inside the action to use a synchronized spliterator.
	 * @return An unsynchronized spliterator, <b>use discouraged</b>
	 */
	@Deprecated
	@Override
	public default Spliterator<I> spliterator() {
		// TODO Auto-generated method stub
		return Iterable.super.spliterator();
	}

}
