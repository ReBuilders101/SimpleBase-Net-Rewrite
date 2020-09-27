package dev.lb.simplebase.net.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Pair<Left, Right> {

	private final Left left;
	private final Right right;
	
	public Pair(Left left, Right right) {
		this.left = left;
		this.right = right;
	}
	
	public Left getLeft() {
		return left;
	}
	
	public Right getRight() {
		return right;
	}
	
	public static <L, R> Consumer<Pair<L, R>> spreading(BiConsumer<L, R> consumer) {
		return (pair) -> consumer.accept(pair.left, pair.right);
	}
}
