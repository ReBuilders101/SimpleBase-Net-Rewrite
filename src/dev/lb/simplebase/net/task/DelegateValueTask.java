package dev.lb.simplebase.net.task;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

final class DelegateValueTask<V, D> implements ValueTask<V> {

	private final ValueTask<D> delegate;
	private final Function<D, V> mapper;
	
	DelegateValueTask(ValueTask<D> delegate, Function<D, V> mapper) {
		this.delegate = delegate;
		this.mapper = mapper;
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
	public <R> ValueTask<R> map(Function<V, R> mapper) {
		return new DelegateValueTask<>(delegate, mapper.compose(this.mapper));
	}
	
	@Override
	public V getSuccess() throws IllegalStateException {
		return mapper.apply(delegate.getSuccess());
	}
	
	@Override
	public ExecutionException getCancelled() throws IllegalStateException {
		return delegate.getCancelled();
	}
	@Override
	public ValueTask<V> tryAwait() throws InterruptedException {
		delegate.tryAwait();
		return this;
	}
	@Override
	public ValueTask<V> tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		delegate.tryAwait(timeout, unit);
		return this;
	}
	@Override
	public ValueTask<V> then(Runnable chainTask) {
		delegate.then(chainTask);
		return this;
	}
	@Override
	public ValueTask<V> thenAsync(Runnable chainTask) {
		delegate.thenAsync(chainTask);
		return this;
	}
	@Override
	public ValueTask<V> onSuccess(Consumer<V> chainTask) {
		delegate.onSuccess((d) -> chainTask.accept(mapper.apply(d)));
		return this;
	}
	@Override
	public ValueTask<V> onCancelled(Consumer<ExecutionException> chainTask) {
		delegate.onCancelled(chainTask);
		return this;
	}
	@Override
	public ValueTask<V> onSuccessAsync(Consumer<V> chainTask) {
		delegate.onSuccessAsync((d) -> chainTask.accept(mapper.apply(d)));
		return this;
	}
	@Override
	public ValueTask<V> onCancelledAsync(Consumer<ExecutionException> chainTask) {
		delegate.onCancelledAsync(chainTask);
		return this;
	}
	
}
