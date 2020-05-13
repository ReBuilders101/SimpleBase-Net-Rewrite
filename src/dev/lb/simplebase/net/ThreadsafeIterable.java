package dev.lb.simplebase.net;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.lb.simplebase.net.annotation.Threadsafe;

/**
 * Provides a way to iterate over an iterable that can be modified from another thread without risking
 * a {@link ConcurrentModificationException} by acquiring and holding a lock/monitor during the iteration.
 * @param <T> The type of the content elements to iterate over
 */
public interface ThreadsafeIterable<T> extends Iterable<T> {

	/**
	 * Iterates over this iterable while holding a lock/monitor to prevent concurrent modification.
	 * The exact type of lock or monitor object depends on the implementation.
	 * Note that as any other operation waiting for a lock/monitor, calling this method carelessly can lead to deadlocks.
	 * Whether the iterator supports the {@link Iterator#remove()} operation depends on the implementation.
	 * @param iteratorHandler The threadsafe operation that accepts an {@link Iterator} to use
	 */
	@Threadsafe
	public void iterate(Consumer<Iterable<T>> iteratorHandler);
	
	/**
	 * Iterates over this iterable while holding a lock/monitor to prevent concurrent modification and returns a result from the iteration
	 * The exact type of lock or monitor object depends on the implementation.
	 * Note that as any other operation waiting for a lock/monitor, calling this method carelessly can lead to deadlocks.
	 * @param iteratorHandler The threadsafe operation that accepts this {@link Iterable} to use and returns a result.
	 * @return The result of the handler function
	 */
	@Threadsafe
	public <R> R iterateReturn(Function<Iterable<T>, R> iteratorHandler);
	
	/**
	 * Applies the conecpt of {@link ThreadsafeIterable} to any {@link Iterable} by synchronizing on an external object.
	 * In this case the object that is used for synchronization is the iterable itself.
	 * @param <T> The type of the content elements to iterate over
	 * @param iterable The iterable that provides an iterator for the handler, and the object used for synchronization
	 * @param handler The threadsafe operation that accepts an {@link Iterator} to use
	 */
	public static <T> void externalSync(Iterable<T> iterable, Consumer<Iterable<T>> handler) {
		synchronized (iterable) {
			handler.accept(iterable);
		}
	}
	
	/**
	 * Applies the conecpt of {@link ThreadsafeIterable} to any {@link Iterable} by synchronizing on an external object.
	 * The lock will be acquired using {@link Lock#lock()} and will always be released when this method exits, even 
	 * when an exception was thrown, by calling {@link Lock#unlock()}.
	 * @param <T> The type of the content elements to iterate over
	 * @param iterable The iterable that provides an iterator for the handler
	 * @param handler The threadsafe operation that accepts an {@link Iterable} to use
	 * @param lock The lock that is used for synchronization
	 */
	public static <T> void externalSync(Iterable<T> iterable, Consumer<Iterable<T>> handler, Lock lock) {
		try {
			lock.lock();
			handler.accept(iterable);
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Applies the conecpt of {@link ThreadsafeIterable} to any {@link Iterable} by synchronizing on an external object.
	 * The object is used for synchronization by acquiring its monitor in a {@code synchronized} block
	 * @param <T> The type of the content elements to iterate over
	 * @param iterable The iterable that provides an iterator for the handler
	 * @param handler The threadsafe operation that accepts an {@link Iterable} to use
	 * @param syncObject The object used for synchronization
	 */
	public static <T> void externalSync(Iterable<T> iterable, Consumer<Iterable<T>> handler, Object syncObject) {
		synchronized (syncObject) {
			handler.accept(iterable);
		}
	}
}
