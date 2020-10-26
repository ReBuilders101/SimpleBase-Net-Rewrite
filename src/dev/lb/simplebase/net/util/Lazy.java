package dev.lb.simplebase.net.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Lazy<T> {

	public T get();
	public <V> Lazy<V> map(Function<T, V> mapper);
	public void ifPresent(Consumer<? super T> action);
	
	public static <T> Lazy<T> of(Supplier<? extends T> supplier) {
		return new ValueLazy<>(supplier);
	}
	
	public static <T> Lazy.Closeable<T> ofCloseable(Supplier<? extends T> supplier, Consumer<? super T> finalizer) {
		return new CloseableLazy<>(supplier, finalizer);
	}
	
	public static <T extends java.io.Closeable> Lazy.Closeable<T> ofCloseable(Supplier<? extends T> supplier) {
		return new CloseableLazy<>(supplier, t -> {
			try {
				t.close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}
	
	public static <T> Lazy.Inline<T> inline() {
		return new InlineLazy<>();
	}
	
	public static <T> Lazy<T> inline(Class<T> type) {
		return inline();
	}
	
	public static interface Inline<T> extends Lazy<T> {
		public T getInline(Supplier<? extends T> supplier);
		public boolean isDefined();
	}
	
	public static interface Closeable<T> extends Lazy<T>, java.io.Closeable {
		@Override public void close(); //Redeclare without exception
		public void ifOpen(Consumer<? super T> action);
	}
}
