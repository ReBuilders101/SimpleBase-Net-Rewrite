package dev.lb.simplebase.net.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Stores an immutable set of two values.
 * @param <Left> The type of the left/first value
 * @param <Right> The type of the right/second value
 */
public class Pair<Left, Right> {

	private final Left left;
	private final Right right;
	
	/**
	 * Creates a new {@link Pair} with a left and right value
	 * @param left The left/first value of the pair
	 * @param right The right/second value of the pair
	 */
	public Pair(Left left, Right right) {
		this.left = left;
		this.right = right;
	}
	
	/**
	 * The left/first value of this pair
	 * @return The left/first value of this pair
	 */
	public Left getLeft() {
		return left;
	}
	
	/**
	 * The right/second value of this pair
	 * @return The right/second value of this pair
	 */
	public Right getRight() {
		return right;
	}
	
	/**
	 * Wraps a {@link BiConsumer} of two types into a {@link Consumer} accepting a pair
	 * of these two types
	 * @param <L> The type of the left value in the pair, and the first parameter of the {@link BiConsumer}
	 * @param <R> The type of the right value in the pair, and the second parameter of the {@link BiConsumer}
	 * @param consumer The {@link BiConsumer} that should be wrapped in a {@link Consumer}{@code <}{@link Pair}{@code >}.
	 * @return A {@link Consumer}{@code <}{@link Pair}{@code >} that wraps the {@link BiConsumer}
	 */
	public static <L, R> Consumer<Pair<L, R>> spreading(BiConsumer<L, R> consumer) {
		return (pair) -> consumer.accept(pair.left, pair.right);
	}
}
