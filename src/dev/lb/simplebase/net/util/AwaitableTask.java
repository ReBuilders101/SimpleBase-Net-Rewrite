package dev.lb.simplebase.net.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.lb.simplebase.net.annotation.Threadsafe;


@Threadsafe
public class AwaitableTask implements Task {

	private final CountDownLatch waiter;
	private final List<Runnable> completeTasks;
	
	public AwaitableTask() {
		this.waiter = new CountDownLatch(1);
		this.completeTasks = new ArrayList<>();
	}
	
	@Override
	public boolean isDone() {
		return waiter.getCount() == 0;
	}

	@Override
	public boolean isSynchrounous() {
		return false;
	}

	@Override
	public void tryAwait() throws InterruptedException {
		waiter.await();
	}

	@Override
	public void tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		if(!waiter.await(timeout, unit)) {
			throw new TimeoutException();
		}
	}

	//Sync these two to ensure that all chain tasks are run
	
	@Override
	public synchronized Task then(Runnable chainTask) {
		if(waiter.getCount() == 0) {
			chainTask.run();
		} else {
			completeTasks.add(chainTask);
		}
		return this;
	}

	public synchronized void release() {
		waiter.countDown();
		completeTasks.forEach(Runnable::run);
	}
	
}
