package dev.lb.simplebase.net.util;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MonitorBasedThreadsafeIterable<T, I> implements ThreadsafeIterable<T, I> {

	private final Object monitor;
	private final T object;
	private final Supplier<Iterable<I>> iterable;
	
	public MonitorBasedThreadsafeIterable(Object monitor, T object, Supplier<Iterable<I>> iterable) {
		this.monitor = monitor;
		this.object = object;
		this.iterable = iterable;
	}

	@Override
	public void action(Consumer<T> action) {
		synchronized (monitor) { //Run the action while holding monitor
			action.accept(object);
		}
	}

	@Override
	public <R> R actionReturn(Function<T, R> action) {
		synchronized (monitor) { //Run the action while holding monitor
			return action.apply(object); //Return here, will release monitor on its own
		}
	}

	@Override
	public void forEach(Consumer<? super I> itemAction) {
		synchronized (monitor) { //Iterate backing set while holding the monitor
			iterable.get().forEach(itemAction);
		}
	}

	@Override
	public Iterator<I> iterator() {
		if(Thread.holdsLock(monitor)) { //If it is held by this thread then it is held somewhere up the stack -> 
			return iterable.get().iterator(); //so we can return an instance to the caller (relatively) safely
		} else {
			throw new IllegalStateException("Current thread does not hold object monitor"); //No lock, no iterator
		}
	}

	@Override
	public Spliterator<I> spliterator() {
		if(Thread.holdsLock(monitor)) { //If it is held by this thread then it is held somewhere up the stack -> 
			return iterable.get().spliterator(); //so we can return an instance to the caller (relatively) safely
		} else {
			throw new IllegalStateException("Current thread does not hold object monitor"); //No lock, no iterator
		}
	}

	@Override
	public <R> Optional<R> forEachReturn(Function<? super I, Optional<R>> itemFunction) {
		synchronized (monitor) {
			for(I i : iterable.get()) {
				Optional<R> val = itemFunction.apply(i);
				if(val.isPresent()) return val;
			}
			return Optional.empty();
		}
	}

}
