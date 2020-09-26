package dev.lb.simplebase.net.task;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import dev.lb.simplebase.net.task.AwaitableValueTask.CompletionSource;
import dev.lb.simplebase.net.util.Pair;

public interface ValueTask<V> extends Task {

	public default boolean isSuccess() {
		return isDone() && !isCancelled();
	}

	public boolean isCancelled();
	
	@SuppressWarnings("unchecked")
	public default <E extends Exception> ValueTask<V> throwInner(Class<E> innerExceptionType) throws E, ClassCastException {
		if(isCancelled()) {
			final Throwable ex = getCancelled().getCause();
			if(innerExceptionType.isInstance(ex)) {
				throw (E) ex;
			} else {
				throw new ClassCastException("ValueTask exception is of type " + ex.getClass() + ", not " + innerExceptionType);
			}
		}
		return this;
	}
	
	public default ValueTask<V> ifSuccess(Consumer<V> function) {
		if(isSuccess()) {
			function.accept(getSuccess());
		}
		return this;
	}
	
	public default ValueTask<V> ifCancelled(Consumer<ExecutionException> handler) {
		if(isCancelled()) {
			handler.accept(getCancelled());
		} 
		return this;
	}
	
	public default <R> ValueTask<R> map(Function<V, R> mapper) {
		return new DelegateValueTask<>(this, mapper);
	}
	
	public default V get() throws InterruptedException, ExecutionException {
		tryAwait();
		if(isSuccess()) {
			return getSuccess();
		} else {
			throw getCancelled();
		}
	}
	
	public V getSuccess() throws IllegalStateException;
	public ExecutionException getCancelled() throws IllegalStateException;
	
	public default Optional<V> getOptional() throws InterruptedException {
		tryAwait();
		return getOptionalNow();
	}
	
	public default Optional<V> getOptionalNow() {
		if(isSuccess()) {
			return Optional.ofNullable(getSuccess());
		} else {
			return Optional.empty();
		}
	}
	
	public default Stream<V> streamSuccess() {
		if(isSuccess()) {
			return Stream.of(getSuccess());
		} else {
			return Stream.empty();
		}
	}

	@Override
	public default ValueTask<V> await() {
		Task.super.await();
		return this;
	}

	@Override
	public default ValueTask<V> await(long timeout, TimeUnit unit) {
		Task.super.await(timeout, unit);
		return this;
	}

	@Override
	public ValueTask<V> tryAwait() throws InterruptedException;

	@Override
	public ValueTask<V> tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;

	@Override
	public ValueTask<V> then(Runnable chainTask);

	@Override
	public ValueTask<V> thenAsync(Runnable chainTask);
	
	public ValueTask<V> onSuccess(Consumer<V> chainTask);
	public ValueTask<V> onCancelled(Consumer<ExecutionException> chainTask);
	public default ValueTask<V> onCompleted(Consumer<V> successTask, Consumer<ExecutionException> cancelledTask) {
		onSuccess(successTask);
		onCancelled(cancelledTask);
		return this;
	}
	
	public ValueTask<V> onSuccessAsync(Consumer<V> chainTask);
	public ValueTask<V> onCancelledAsync(Consumer<ExecutionException> chainTask);
	public default ValueTask<V> onCompletedAsync(Consumer<V> successTask, Consumer<ExecutionException> cancelledTask) {
		onSuccessAsync(successTask);
		onCancelledAsync(cancelledTask);
		return this;
	}
	
	
	
	public static <V> ValueTask<V> success(V value) {
		return new SuccessValueTask<>(value);
	}
	
	public static ValueTask<?> cancelled(Throwable cause) {
		return new CancelledValueTask<>(cause);
	}
	
	public static <V> ValueTask<V> cancelled(Throwable cause, Class<V> expectedValueType) {
		return new CancelledValueTask<>(cause);
	}
	
	public static <V> Pair<ValueTask<V>, CompletionSource<V>> completable() {
		final AwaitableValueTask<V> task = new AwaitableValueTask<>();
		return new Pair<>(task, task.createCompletionSource());
	}
} 
