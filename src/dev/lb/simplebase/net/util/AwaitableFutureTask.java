package dev.lb.simplebase.net.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AwaitableFutureTask implements Task {

	private final Future<?> future;
	
	public AwaitableFutureTask(Future<?> future) {
		this.future = future;
	}
	
	@Override
	public boolean isDone() {
		return future.isDone();
	}

	@Override
	public boolean isSynchrounous() {
		return false;
	}

	@Override
	public void tryAwait() throws InterruptedException {
		try {
			future.get();
		} catch (ExecutionException e) {
			//Do nothing. Just return.
		}
	}

	@Override
	public void tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		try {
			future.get(timeout, unit);
		} catch (ExecutionException e) {
			//Do nothing. Just return.
		}
	}

	@Override
	public Task then(Runnable chainTask) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<?> asFuture() {
		return future;
	}

}
