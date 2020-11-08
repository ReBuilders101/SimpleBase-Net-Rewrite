package dev.lb.simplebase.net.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.lb.simplebase.net.annotation.Threadsafe;

/**
 * Provides a way to iterate over an iterable that can be modified from another thread without risking
 * a {@link ConcurrentModificationException} by acquiring and holding a lock/monitor during the iteration.
 * @see ThreadsafeAction
 * @param <T> The type of the ThreadsafeIterable implementation class
 * @param <I> The type of the content elements to iterate over
 */
@Threadsafe
public interface ThreadsafeIterable<T, I> extends ThreadsafeAction<T> {

	/**
	 * Iterates over this iterable while holding a lock/monitor to prevent concurrent modification.
	 * The exact type of lock or monitor object depends on the implementation.
	 * As any other operation waiting for a lock/monitor, calling this method carelessly can lead to deadlocks.
	 * @param itemAction The threadsafe operation that should be executed for every item in the iterable
	 */
	@Threadsafe
	public void forEach(Consumer<? super I> itemAction);

	/**
	 * Iterates over this iterable while holding a lock/monitor to prevent concurrent modification.
	 * The exact type of lock or monitor object depends on the implementation.
	 * As any other operation waiting for a lock/monitor, calling this method carelessly can lead to deadlocks.
	 * <p>
	 * If the function returns an empty optional for an item, the iteration will continue with the next item.
	 * If the function returns an optional with a value, the iteration will stop and the optional will be returned.
	 * If no iteration step produces an optional with a value, this method returns an empty optional.
	 * @param <R> The type of the return value
	 * @param itemFunction The threadsafe operation that should be executed for every item in the iterable
	 * @return An {@link Optional} with the returned value, or an empty one if no value was found
	 */
	@Threadsafe
	public <R> Optional<R> forEachReturn(Function<? super I, Optional<R>> itemFunction);
	
	/**
	 * Creates a new {@link Iterator} for this object only if the current thread already holds the lock/monitor of
	 * this object. Otherwise, it throws an Exception.
	 * @return A new {@link Iterator} for this object
	 * @throws IllegalStateException If the current thread does not already own the lock/monitor
	 */
	@Threadsafe
	public Iterator<I> iterator();
	
	/**
	 * Creates a new {@link Spliterator} for this object only if the current thread already holds the lock/monitor of
	 * this object. Otherwise, it throws an Exception.
	 * @return A new {@link Spliterator} for this object
	 * @throws IllegalStateException If the current thread does not already own the lock/monitor
	 */
	@Threadsafe
	public Spliterator<I> spliterator();

}
