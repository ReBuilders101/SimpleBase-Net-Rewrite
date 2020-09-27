package dev.lb.simplebase.net.task;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;

@Internal
@Threadsafe
final class AwaitableValueTask<V> implements ValueTask<V> {

	private static final int RUNNING = 1;
	private static final int SUCCESS = 2;
	private static final int CANCELLED = 3;
	
	private final CountDownLatch waiter;
	private final List<Runnable> successTasks;
	private final List<Runnable> cancelledTasks;
	private final long startTimeStamp;
	private volatile int status;
	private volatile Object checkedData;
	
	AwaitableValueTask() {
		this.waiter = new CountDownLatch(1);
		this.successTasks = new LinkedList<>();
		this.cancelledTasks = new LinkedList<>();
		this.startTimeStamp = NetworkManager.getClockMillis();
		this.status = RUNNING;
		this.checkedData = null;
	}
	
	@Override
	public boolean isDone() {
		return status != RUNNING;
	}

	@Override
	public boolean asyncAwait(long timeout, TimeUnit unit) {
		if(isDone()) return true;
		long timeoutMs = unit.toMillis(timeout);
		return NetworkManager.getClockMillis() > startTimeStamp + timeoutMs;
	}

	@Override
	public boolean isCancelled() {
		return status == CANCELLED;
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized V getSuccess() throws IllegalStateException {
		if(status == SUCCESS) {
			return (V) checkedData;
		} else {
			throw new IllegalStateException("ValueTask was caneclled");
		}
	}

	@Override
	public synchronized ExecutionException getCancelled() throws IllegalStateException {
		if(status == CANCELLED) {
			return (ExecutionException) checkedData;
		} else {
			throw new IllegalStateException("ValueTask was not cancelled");
		}
	}

	@Override
	public ValueTask<V> tryAwait() throws InterruptedException {
		waiter.await();
		return this;
	}

	@Override
	public ValueTask<V> tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		if(!waiter.await(timeout, unit)) {
			throw new TimeoutException();
		}
		return this;
	}

	@Override
	public synchronized ValueTask<V> then(Runnable chainTask) {
		if(isDone()) {
			chainTask.run();
		} else {
			successTasks.add(chainTask);
			cancelledTasks.add(chainTask);
		}
		return this;
	}

	@Override
	public synchronized ValueTask<V> thenAsync(Runnable chainTask) {
		if(isDone()) {
			CompletableFuture.runAsync(chainTask);
		} else {
			successTasks.add(() -> CompletableFuture.runAsync(chainTask));
			cancelledTasks.add(() -> CompletableFuture.runAsync(chainTask));
		}
		return this;
	}

	@Override
	public synchronized ValueTask<V> onSuccess(Consumer<V> chainTask) {
		if(isSuccess()) {
			chainTask.accept(getSuccess());
		} else if(isRunning()) {
			successTasks.add(() -> chainTask.accept(getSuccess()));
		}
		return this;
	}

	@Override
	public synchronized ValueTask<V> onCancelled(Consumer<ExecutionException> chainTask) {
		if(isCancelled()) {
			chainTask.accept(getCancelled());
		} else if(isRunning()) {
			cancelledTasks.add(() -> chainTask.accept(getCancelled()));
		}
		return this;
	}

	@Override
	public synchronized ValueTask<V> onSuccessAsync(Consumer<V> chainTask) {
		if(isSuccess()) {
			chainTask.accept(getSuccess());
		} else if(isRunning()) {
			successTasks.add(() -> CompletableFuture.runAsync(() -> chainTask.accept(getSuccess())));
		}
		return this;
	}

	@Override
	public ValueTask<V> onCancelledAsync(Consumer<ExecutionException> chainTask) {
		if(isCancelled()) {
			chainTask.accept(getCancelled());
		} else if(isRunning()) {
			successTasks.add(() -> CompletableFuture.runAsync(() -> chainTask.accept(getCancelled())));
		}
		return this;
	}
	
	public synchronized void success(V value) {
		if(status != RUNNING) {
			throw new IllegalStateException("Cannot complete a task twice");
		}
		
		status = SUCCESS;
		checkedData = value;
		successTasks.forEach(Runnable::run);
		waiter.countDown();
	}
	
	public synchronized void cancelled(Throwable cause) {
		if(status != RUNNING) {
			throw new IllegalStateException("Cannot complete a task twice");
		}
		
		status = CANCELLED;
		checkedData = new ExecutionException(cause);
		cancelledTasks.forEach(Runnable::run);
		waiter.countDown();
	}
	
	public ValueTask.CompletionSource<V> createCompletionSource() {
		return new CompletionSource<>(this);
	}

}
