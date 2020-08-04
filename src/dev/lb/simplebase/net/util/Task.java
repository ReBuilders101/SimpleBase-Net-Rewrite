package dev.lb.simplebase.net.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * An action that may execute asynchronously.
 */
public interface Task {

	public boolean isDone();
	
	public boolean isSynchrounous();
	
	public default boolean await() {
		try {
			tryAwait();
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	public void tryAwait() throws InterruptedException;
	public void tryAwait(long timeout, TimeUnit unit) throws InterruptedException;
	
	public Task then(Runnable chainTask);
	
	public default Future<?> asFuture() {
		return new Future<Object>() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public boolean isDone() {
				return Task.this.isDone();
			}

			@Override
			public Object get() throws InterruptedException, ExecutionException {
				Task.this.tryAwait();
				return null;
			}

			@Override
			public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				Task.this.tryAwait(timeout, unit);
				return null;
			}
		};
	}
	
	public default <T> Future<T> asFuture(Supplier<T> result) {
		return new Future<T>() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public boolean isDone() {
				return Task.this.isDone();
			}

			@Override
			public T get() throws InterruptedException, ExecutionException {
				Task.this.tryAwait();
				return result.get();
			}

			@Override
			public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				Task.this.tryAwait(timeout, unit);
				return result.get();
			}
		};
	}
}
