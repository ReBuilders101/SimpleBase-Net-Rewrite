package dev.lb.simplebase.net.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class CloseableLazy<T> implements Lazy.Closeable<T> {

	private final Consumer<? super T> finalizer;
	private final Object lock = new Object();
	private volatile boolean closed;
	private Supplier<? extends T> supplier;
	private T value;

	CloseableLazy(Supplier<? extends T> supplier, Consumer<? super T> finalizer) {
		Objects.requireNonNull(supplier, "Lazy value supplier must not be null");
		Objects.requireNonNull(finalizer, "CloseableLazy value finalizer must not be null");
		this.finalizer = finalizer;
		this.closed = false;
		this.supplier = supplier;
		this.value = null;
	}
	
	@Override
	public T get() {
		if(closed) {
			throw new IllegalStateException("CloseableLazy is closed");
		} else if(supplier != null) {
			synchronized (lock) {
				//Re-check whether another thread already read the value or closed this
				if(closed) {
					throw new IllegalStateException("CloseableLazy is closed");
				} else if(supplier != null) {
					//Writing is synchronized, reading not
					value = supplier.get();
					supplier = null;
				}
			}
		}
		return value;
	}

	@Override
	public <V> Lazy<V> map(Function<T, V> mapper) {
		return new DelegateLazy<>(this, mapper);
	}

	@Override
	public void close() {
		if(closed) return; //No need to lock
		synchronized (lock) {
			if(value != null) {
				finalizer.accept(value);
			}
			closed = true;
		}
	}

	@Override
	public void ifPresent(Consumer<? super T> action) {
		if(supplier == null) { //This will never change as there is no unGet() or sth
			action.accept(value);
		}
	}

	@Override
	public void ifOpen(Consumer<? super T> action) {
		if(supplier == null) {
			if(closed) return;
			synchronized (lock) {
				if(!closed) {
					action.accept(value);
				}
			}
		}
	}
}
