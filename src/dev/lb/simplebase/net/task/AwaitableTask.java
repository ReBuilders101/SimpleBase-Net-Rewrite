package dev.lb.simplebase.net.task;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;


/**
 * Internal. Use {@link Task#completable()} instead.
 */
@Internal
@Threadsafe
public class AwaitableTask implements Task {

	private final CountDownLatch waiter;
	private final List<Runnable> completeTasks;
	private final long startTimeStamp;
	
	/**
	 * Internal. Use {@link Task#completable()} instead.
	 */
	public AwaitableTask() {
		this(1);
	}
	
	protected AwaitableTask(int counter) {
		this.waiter = new CountDownLatch(counter);
		this.completeTasks = new LinkedList<>();
		this.startTimeStamp = NetworkManager.getClockMillis();
	}
	
	@Override
	public boolean isDone() {
		return waiter.getCount() == 0;
	}

	@Override
	public Task tryAwait() throws InterruptedException {
		waiter.await();
		return this;
	}

	@Override
	public Task tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		if(!waiter.await(timeout, unit)) {
			throw new TimeoutException();
		}
		return this;
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

	@Override
	public Task thenAsync(Runnable chainTask) {
		if(waiter.getCount() == 0) {
			CompletableFuture.runAsync(chainTask);
		} else {
			completeTasks.add(() -> CompletableFuture.runAsync(chainTask));
		}
		return this;
	}
	
	/**
	 * Completes the task
	 */
	public synchronized void release() {
		completeTasks.forEach(Runnable::run);
		waiter.countDown();
	}

	@Override
	public boolean asyncAwait(long timeout, TimeUnit unit) {
		if(isDone()) return true;
		long timeoutMs = unit.toMillis(timeout);
		return NetworkManager.getClockMillis() > startTimeStamp + timeoutMs;
	}
	
}
