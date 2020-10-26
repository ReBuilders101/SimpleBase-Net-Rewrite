package dev.lb.simplebase.net.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementations of the {@link Lazy} interface store a {@link Supplier} for a value that is lazily evaluated when required.
 * The retrieved value is then cached for faster access.
 * <p>
 * All implementations of the {@link Lazy} interface are guaranteed to be threadsafe.
 * </p>
 * @param <T> The type of the stored value
 */
public interface Lazy<T> {

	/**
	 * If a value is already present, that value will be returned.
	 * Otherwise, the stored {@link Supplier} will be called and the value
	 * returned by the supplier will be stored in the {@link Lazy} and returned.
	 * <p>
	 * Calling the {@code Supplier} when no value is present may require the implementation to wait for a lock or monitor
	 * to ensure thread safety, but calling this method when a value is already stored will never require waiting for any locks or monitors.
	 * </p>
	 * @return The value stored in the {@link Lazy}
	 */
	public T get();
	
	/**
	 * Creates a new {@link Lazy} that encapsulates the value produced by applying a mapper function to this {@code Lazy}'s value.
	 * <p>
	 * The returned {@code Lazy}'s state (whether the value has been resolved) is initially independent of this {@code Lazy}.
	 * However, resolving the returned {@code Lazy} by calling {@link #get()} will also resolve the value of this {@code Lazy}.
 	 * </p>
	 * @param <V> The type that the lazy will be mapped to
	 * @param mapper The mapping function that gets the mapped value from this lazy's value
	 * @return The mapped {@link Lazy} implementation
	 */
	public <V> Lazy<V> map(Function<T, V> mapper);
	
	/**
	 * Runs a {@link Consumer} with the stored value only if a value is present in this {@code Lazy}.
	 * @param action The action to run with the value of this {@code Lazy} 
	 */
	public void ifPresent(Consumer<? super T> action);
	
	/**
	 * Creates a new {@link Lazy} that will retrieve its value from a {@link Supplier}.
	 * <br>
	 * The supplier will only ever be called once.
	 * @param <T> The type of the stored value 
	 * @param supplier The {@link Supplier} that can supply the value to store.
	 * @return The {@code Lazy} for that {@code Supplier}
	 */
	public static <T> Lazy<T> of(Supplier<? extends T> supplier) {
		return new ValueLazy<>(supplier);
	}
	
	/**
	 * Creates a new {@link Lazy.Closeable} for any type of value using a custom close function.
	 * @param <T> The type of the stored value
	 * @param supplier The {@link Supplier} that can supply the value to store
	 * @param closeFunc The function to call on the stored value when {@link Lazy.Closeable#close()} is called.
	 * @return The created {@link Lazy} implementation
	 */
	public static <T> Lazy.Closeable<T> ofCloseable(Supplier<? extends T> supplier, Consumer<? super T> closeFunc) {
		return new CloseableLazy<>(supplier, closeFunc);
	}
	
	/**
	 * Creates a new {@link Lazy.Closeable} for an object that implements the {@code java.io.Closeable} interface.
	 * <p>
	 * When {@link Lazy.Closeable#close()} is called, the value's {@code close()} method will be called.
	 * Any {@link IOException} that occurs will be wrapped in an {@link UncheckedIOException}.
	 * </p>
	 * @param <T> The type of the stored value
	 * @param supplier The {@link Supplier} that can supply the value to store
	 * @return The created {@link Lazy} implementation
	 */
	public static <T extends java.io.Closeable> Lazy.Closeable<T> ofCloseable(Supplier<? extends T> supplier) {
		return new CloseableLazy<>(supplier, t -> {
			try {
				t.close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}
	
	/**
	 * Creates a new {@link Lazy.Inline}.
	 * No supplier is required  to create an instance as that will be set with {@link Lazy.Inline#getInline(Supplier)}.
	 * @param <T> The type of the stored value
	 * @return The created {@link Lazy} implementation
	 */
	public static <T> Lazy.Inline<T> inline() {
		return new InlineLazy<>();
	}
	
	/**
	 * Creates a new {@link Lazy.Inline}.
	 * No supplier is required  to create an instance as that will be set with {@link Lazy.Inline#getInline(Supplier)}.
	 * <p>
	 * Can be used instead of {@link #inline()} where the compiler can't infer the generic type of the {@link Lazy}.
	 * </p>
	 * @param <T> The type of the stored value
	 * @param type The {@link Class} of the stored value's type
	 * @return The created {@link Lazy} implementation
	 */
	public static <T> Lazy<T> inline(Class<T> type) {
		return inline();
	}
	
	/**
	 * A special type of {@link Lazy} that defines the value supplier at the same place where the
	 * value is retrieved.<br>
	 * Can be used to create a cached method-local variable.
	 * @param <T> The type of the stored value
	 */
	public static interface Inline<T> extends Lazy<T> {
		/**
		 * Defines the {@link Supplier} that creates the value and then immediately
		 * stores the value from the supplier in the {@code Lazy}. If a value is already present in this {@code Lazy},
		 * the {@code Supplier} will never be called and the stored value is returned immediately.
		 * @param supplier The {@link Supplier} that can supply the value to store
		 * @return The value of the {@code Lazy}, either from the supplier or the stored value
		 */
		public T getInline(Supplier<? extends T> supplier);
		/**
		 * Checks whether this {@link Lazy} has been initialized by calling {@link #getInline(Supplier)} with a supplier at least
		 * once. If this method returns {@code false}, calling {@link #get()} will result in a {@link NoSuchMethodError}.
		 * @return {@code true} if a {@link Supplier} has been set for this {@link Lazy}.
		 */
		public boolean isDefined();
	}
	
	/**
	 * A special type of {@link Lazy} that declares an additional {@link #close()} method.
	 * <p>
	 * Can be used to encapsulate a value that implements the {@link java.io.Closeable} interface and
	 * to call the {@code close()} method on such objects only if the value of the {@code Lazy} is actually present.
	 * </p>
	 * @param <T> The type of the stored value
	 */
	public static interface Closeable<T> extends Lazy<T>, java.io.Closeable {
		/**
		 * Calls the closing method that was specified when this {@code Lazy.Closeable} was created
		 * only when the value is present.
		 * <p>
		 * Calling this method more than once has no effect. Trying to retrieve a value with the {@link #get()}
		 * method after calling {@code close()} will result in an exception being thrown.
		 * </p>
		 * <p>
		 * While implementing {@link java.io.Closeable#close()}, this method does not declare an {@link IOException}.
		 * If the contained object's close method throws an {@code IOException} (or a subtype exception), it will be
		 * wrapped in an {@link UncheckedIOException} and thrown by this method. Other unchecked exception types will not be wrapped.
		 * </p>
		 */
		@Override public void close(); //Redeclare without exception
		
		/**
		 * Runs a {@link Consumer} with the stored value only if a value is present and this {@code Lazy} has not yet been closed.
		 * @param action The action to run with the value of this {@code Lazy} 
		 */
		public void ifOpen(Consumer<? super T> action);
	}
}
