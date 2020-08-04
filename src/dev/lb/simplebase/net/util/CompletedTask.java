package dev.lb.simplebase.net.util;

import java.util.concurrent.TimeUnit;

public class CompletedTask implements Task {

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public boolean isSynchrounous() {
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

}
