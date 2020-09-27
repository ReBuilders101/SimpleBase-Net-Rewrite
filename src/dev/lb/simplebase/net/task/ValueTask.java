package dev.lb.simplebase.net.task;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

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

	public static <A, B> ValueTask.PairTask<A, B> ofPair(ValueTask<Pair<A, B>> delegate) {
		return new PairTask<>(delegate);
	}





	public static final class CompletionSource<V> {

		private final AwaitableValueTask<V> task;

		CompletionSource(AwaitableValueTask<V> task) {
			this.task = task;
		}

		public void success(V value) {
			task.success(value);
		}

		public void cancelled(Throwable cause) {
			task.cancelled(cause);
		}

		public boolean isSet() {
			return task.isDone();
		}

	}

	public static class PairTask<A, B> implements ValueTask<Pair<A, B>> {

		private final ValueTask<Pair<A, B>> delegate;

		protected PairTask(ValueTask<Pair<A, B>> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean isDone() {
			return delegate.isDone();
		}

		@Override
		public boolean asyncAwait(long timeout, TimeUnit unit) {
			return delegate.asyncAwait(timeout, unit);
		}

		@Override
		public boolean isCancelled() {
			return delegate.isCancelled();
		}

		@Override
		public Pair<A, B> getSuccess() throws IllegalStateException {
			return delegate.getSuccess();
		}

		public A getSuccessLeft() throws IllegalStateException {
			return delegate.getSuccess().getLeft();
		}

		public B getSuccessRight() throws IllegalStateException {
			return delegate.getSuccess().getRight();
		}

		@Override
		public ExecutionException getCancelled() throws IllegalStateException {
			return delegate.getCancelled();
		}

		@Override
		public ValueTask.PairTask<A, B> tryAwait() throws InterruptedException {
			delegate.tryAwait();
			return this;
		}

		@Override
		public ValueTask.PairTask<A, B> tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
			delegate.tryAwait(timeout, unit);
			return this;
		}

		@Override
		public ValueTask.PairTask<A, B> then(Runnable chainTask) {
			delegate.then(chainTask);
			return this;
		}

		@Override
		public ValueTask.PairTask<A, B> thenAsync(Runnable chainTask) {
			delegate.thenAsync(chainTask);
			return this;
		}

		@Override
		public ValueTask.PairTask<A, B> onSuccess(Consumer<Pair<A, B>> chainTask) {
			delegate.onSuccess(chainTask);
			return this;
		}

		public ValueTask.PairTask<A, B> onSuccess(BiConsumer<A, B> chainTask) {
			return onSuccess(Pair.spreading(chainTask));
		}

		@Override
		public ValueTask.PairTask<A, B> onCancelled(Consumer<ExecutionException> chainTask) {
			delegate.onCancelled(chainTask);
			return this;
		}

		@Override
		public ValueTask.PairTask<A, B> onSuccessAsync(Consumer<Pair<A, B>> chainTask) {
			delegate.onSuccessAsync(chainTask);
			return this;
		}

		public ValueTask.PairTask<A, B> onSuccessAsync(BiConsumer<A, B> chainTask) {
			return onSuccessAsync(Pair.spreading(chainTask));
		}

		@Override
		public ValueTask.PairTask<A, B> onCancelledAsync(Consumer<ExecutionException> chainTask) {
			delegate.onCancelledAsync(chainTask);
			return this;
		}

		@Override
		public <E extends Exception> ValueTask.PairTask<A, B> throwInner(Class<E> innerExceptionType) throws E, ClassCastException {
			delegate.throwInner(innerExceptionType);
			return this;
		}

		@Override
		public ValueTask.PairTask<A, B> ifSuccess(Consumer<Pair<A, B>> function) {
			delegate.ifSuccess(function);
			return this;
		}

		public ValueTask.PairTask<A, B> ifSuccess(BiConsumer<A, B> function) {
			return ifSuccess(Pair.spreading(function));
		}

		@Override
		public ValueTask.PairTask<A, B> ifCancelled(Consumer<ExecutionException> handler) {
			delegate.ifCancelled(handler);
			return this;
		}

		@Override
		public ValueTask.PairTask<A, B> await() {
			delegate.await();
			return this;
		}

		@Override
		public ValueTask.PairTask<A, B> await(long timeout, TimeUnit unit) {
			delegate.await(timeout, unit);
			return this;
		}

		@Override
		public ValueTask.PairTask<A, B> onCompleted(Consumer<Pair<A, B>> successTask, Consumer<ExecutionException> cancelledTask) {
			delegate.onCompleted(successTask, cancelledTask);
			return this;
		}

		public ValueTask.PairTask<A, B> onCompleted(BiConsumer<A, B> successTask, Consumer<ExecutionException> cancelledTask) {
			return onCompleted(Pair.spreading(successTask), cancelledTask);
		}

		@Override
		public ValueTask.PairTask<A, B> onCompletedAsync(Consumer<Pair<A, B>> successTask,	Consumer<ExecutionException> cancelledTask) {
			delegate.onCompletedAsync(successTask, cancelledTask);
			return this;
		}

		public ValueTask.PairTask<A, B> onCompletedAsync(BiConsumer<A, B> successTask,	Consumer<ExecutionException> cancelledTask) {
			return onCompletedAsync(Pair.spreading(successTask), cancelledTask);
		}

	}
} 
