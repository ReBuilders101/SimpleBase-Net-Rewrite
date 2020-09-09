package dev.lb.simplebase.net.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import dev.lb.simplebase.net.annotation.Internal;

@Internal
class CompletedTask implements Task {

	//CompletedTask is stateless, so we can use one instance
	static final CompletedTask INSTANCE = new CompletedTask();
	
	private CompletedTask() {}
	
	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public void tryAwait() throws InterruptedException {
		return;
	}

	@Override
	public void tryAwait(long timeout, TimeUnit unit) throws InterruptedException {
		return;
	}

	@Override
	public Task then(Runnable chainTask) {
		chainTask.run();
		return this;
	}

	@Override
	public Task thenAsync(Runnable chainTask) {
		CompletableFuture.runAsync(chainTask);
		return this;
	}

	@Override
	public boolean asyncAwait(long timeout, TimeUnit unit) {
		return true;
	}

}
