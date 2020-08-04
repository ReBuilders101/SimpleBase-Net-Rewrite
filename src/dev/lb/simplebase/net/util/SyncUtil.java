package dev.lb.simplebase.net.util;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class SyncUtil {

	public static <T> void await(T object, Predicate<T> condition) {
		while(!condition.test(object)) {
			if(Thread.interrupted()) return;
			Thread.yield();
		}
	}
	
	public static <T> void doAndAwait(T object, Consumer<T> action, Predicate<T> condition) {
		action.accept(object);
		await(object, condition);
	}
	
	public static <T> void tryAwait(T object, Predicate<T> condition) throws InterruptedException {
		while(!condition.test(object)) {
			if(Thread.interrupted()) throw new InterruptedException();
			Thread.yield();
		}
	}
	
	public static <T> void tryDoAndAwait(T object, Consumer<T> action, Predicate<T> condition) throws InterruptedException {
		action.accept(object);
		tryAwait(object, condition);
	}
}
