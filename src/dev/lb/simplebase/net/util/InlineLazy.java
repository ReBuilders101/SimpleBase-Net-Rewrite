package dev.lb.simplebase.net.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class InlineLazy<T> implements Lazy.Inline<T> {

	private Supplier<? extends T> supplier;
	private T value;
	private boolean initialized;
	
	private final Object lock = new Object();
	
	protected  InlineLazy() {
		this.supplier = null;
		this.value = null;
		this.initialized = false;
	}
	
	@Override
	public T get() {
		if(!initialized) {
			throw new NoSuchMethodError("No definition for inline lazy supplier");
		}
		
		if(supplier != null) {
			synchronized (lock) {
				if(supplier != null) {
					this.value = supplier.get();
					this.supplier = null;
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
	public void ifPresent(Consumer<? super T> action) {
		if(supplier == null && initialized) {
			action.accept(value);
		}
	}

	@Override
	public T getInline(Supplier<? extends T> supplier) {
		Objects.requireNonNull(supplier, "Inline Lazy Supplier must not be null");
		if(!initialized) {
			synchronized (lock) {
				if(!initialized) {
					this.supplier = supplier;
				}
			}
		}
		return get();
	}

	@Override
	public boolean isDefined() {
		return initialized;
	}

}
