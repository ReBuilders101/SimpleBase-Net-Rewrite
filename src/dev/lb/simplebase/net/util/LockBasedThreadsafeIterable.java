package dev.lb.simplebase.net.util;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import dev.lb.simplebase.net.annotation.Internal;

@Internal
@SuppressWarnings("javadoc")
public final class LockBasedThreadsafeIterable<T, I> implements ThreadsafeIterable<T, I>{

	private final T object;
	private final Supplier<Iterable<I>> iterable;
	private final Lock lock;
	private final Boolean defaultBehavior; //Three-state
	
	
	public LockBasedThreadsafeIterable(T object, Supplier<Iterable<I>> iterable, Lock lock, boolean defaultBehavior) {
		this.object = object;
		this.iterable = iterable;
		this.lock = lock;
		this.defaultBehavior = defaultBehavior;
	}
	
	public LockBasedThreadsafeIterable(T object, Supplier<Iterable<I>> iterable, Lock lock) {
		this.object = object;
		this.iterable = iterable;
		this.lock = lock;
		this.defaultBehavior = null;
	}

	@Override
	public void action(Consumer<T> action) {
		try {
			lock.lock();
			action.accept(object);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <R> R actionReturn(Function<T, R> action) {
		try {
			lock.lock();
			return action.apply(object);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void forEach(Consumer<? super I> itemAction) {
		try {
			lock.lock();
			iterable.get().forEach(itemAction);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <R> Optional<R> forEachReturn(Function<? super I, Optional<R>> itemFunction) {
		try {
			lock.lock();
			final Iterable<I> iterable0 = iterable.get();
 			for(I item : iterable0) {
				Optional<R> val = itemFunction.apply(item);
				if(val.isPresent()) return val;
			}
			return Optional.empty();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Iterator<I> iterator() {
		if(hasLock()) {
			return iterable.get().iterator();
		} else {
			throw new IllegalStateException("Current thread does not hold lock"); //No lock, no iterator
		}
	}

	@Override
	public Spliterator<I> spliterator() {
		if(hasLock()) { 
			return iterable.get().spliterator(); 
		} else {
			throw new IllegalStateException("Current thread does not hold lock"); //No lock, no iterator
		}
	}
	
	private boolean hasLock() {
		if(defaultBehavior == null) {
			return LockHelper.isHeldByCurrentThread(lock);
		} else {
			return LockHelper.isHeldByCurrentThread(lock, defaultBehavior.booleanValue());
		}
	}

}
