package dev.lb.simplebase.net.task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

final class SuccessValueTask<V> implements ValueTask<V> {

	private final V value;

	SuccessValueTask(V value) {
		this.value = value;
	}
	
	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public boolean asyncAwait(long timeout, TimeUnit unit) {
		return true;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public V getSuccess() throws IllegalStateException {
		return value;
	}

	@Override
	public ExecutionException getCancelled() throws IllegalStateException {
		throw new IllegalStateException("ValueTask is not cancelled");
	}

	@Override
	public ValueTask<V> tryAwait() throws InterruptedException {
		return this;
	}

	@Override
	public ValueTask<V> tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		return this;
	}

	@Override
	public ValueTask<V> then(Runnable chainTask) {
		chainTask.run();
		return this;
	}

	@Override
	public ValueTask<V> thenAsync(Runnable chainTask) {
		CompletableFuture.runAsync(chainTask);
		return this;
	}

	@Override
	public ValueTask<V> onSuccess(Consumer<V> chainTask) {
		chainTask.accept(value);
		return this;
	}

	@Override
	public ValueTask<V> onCancelled(Consumer<ExecutionException> chainTask) {
		return this;
	}

	@Override
	public ValueTask<V> onSuccessAsync(Consumer<V> chainTask) {
		CompletableFuture.runAsync(() -> chainTask.accept(value));
		return this;
	}

	@Override
	public ValueTask<V> onCancelledAsync(Consumer<ExecutionException> chainTask) {
		return this;
	}
	
}
