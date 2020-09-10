package dev.lb.simplebase.net.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

public class ContextTask<T> implements Task {

	private final Supplier<T> object;
	private final Task task;
	
	private ContextTask(Task task, Supplier<T> object) {
		this.task = task;
		this.object = object;
	}
	
	@Override
	public boolean isDone() {
		return task.isDone();
	}
	
	@Override
	public void tryAwait() throws InterruptedException {
		task.tryAwait();
	}
	
	@Override
	public void tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		task.tryAwait(timeout, unit);
	}
	
	@Override
	public boolean asyncAwait(long timeout, TimeUnit unit) {
		return asyncAwait(timeout, unit);
	}
	
	@Override
	public ContextTask<T> then(Runnable chainTask) {
		task.then(chainTask);
		return this;
	}
	@Override
	public ContextTask<T> thenAsync(Runnable chainTask) {
		task.thenAsync(chainTask);
		return this;
	}
	
	public T get() {
		return object.get();
	}
	
	public <R> ContextTask<R> map(Function<T, R> mapper) {
		return new ContextTask<>(task, () -> mapper.apply(object.get()));
	}
	
	public static <T> ContextTask<T> of(Task task, T object) {
		return new ContextTask<>(task, () -> object);
	}
	
	public static <T, R> ContextTask<R> of(Task task, T object, Function<T, R> property) {
		return new ContextTask<>(task, () -> property.apply(object));
	}
}
