package dev.lb.simplebase.net.util;

import java.util.function.Function;
import java.util.function.Supplier;

public interface Lazy<T> {

	public T get();
	public <V> Lazy<V> map(Function<T, V> mapper);
	
	
	public static <T> Lazy<T> of(Supplier<T> supplier) {
		return new ValueLazy<>(supplier);
	}
}
