package dev.lb.simplebase.net.util;

import java.util.Objects;
import java.util.function.Supplier;

import dev.lb.simplebase.net.annotation.Threadsafe;

/**
 * Container for a single value that is lazily populated when first retrieved.
 * @param <T> The contained type
 */
@Threadsafe
public final class Lazy<T> {

	private Supplier<T> supplier;
	private T value;
	
	private final Object lock = new Object();
	
	/**
	 * Creates a new instance
	 * @param supplier The value supplier. Must not be null. Will be called only once.
	 */
	public Lazy(Supplier<T> supplier) {
		Objects.requireNonNull(supplier, "Lazy value supplier must not be null");
		this.supplier = supplier;
		this.value = null;
	}
	
	/**
	 * Get the contained value. Either a cachen value, or calls the supplier and stores the result
	 * @return The value
	 */
	public T get() {
		//Supplier exists, must retrieve value
		if(supplier != null) {
			synchronized (lock) {
				//Re-check whether another thread already read the value
				if(supplier != null) {
					//Writing is synchronized, reading not
					value = supplier.get();
					supplier = null;
				}
			}
		}
		return value;
	}
	
}
