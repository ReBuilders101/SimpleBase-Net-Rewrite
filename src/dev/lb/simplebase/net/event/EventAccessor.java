package dev.lb.simplebase.net.event;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import dev.lb.simplebase.net.annotation.Threadsafe;

/**
 * Provides access to the handlers of an event for a specific object.<br>
 * Comparable to a .NET event member
 * @param <E> The event type for this accessor
 */
@Threadsafe
public class EventAccessor<E extends Event> { //NOT Iterable on purpose!!

	//Handlers are rarely added, but events can be posted from any amount of threads at a time, requiring iteration
	private final ReadWriteLock lockHandlers;
	private final Set<EventHandler<E>> handlers;
	private final Class<E> eventClass;
	
	public EventAccessor(Class<E> eventClass) {
		Objects.requireNonNull(eventClass, "'eventClass' Parameter must not be null");
		this.eventClass = eventClass;
		this.lockHandlers = new ReentrantReadWriteLock();
		this.handlers = new TreeSet<>(); //Use the natural order to sort by priority
	}
	
	/**
	 * Adds a handler that will be called when this event is posted.<br>
	 * This method behaves like {@link Set#add(Object)}
	 * @param handler The new handler
	 * @return {@code true} when the handler was added
	 */
	public boolean addHandler(Consumer<E> handler) {
		Objects.requireNonNull(handler, "'handler' Parameter must not be null");
		try {
			lockHandlers.writeLock().lock();
			final EventHandler<E> eh = new EventHandler<>(handler, EventHandlerPriority.NORMAL, false);
			return handlers.add(eh);
		} finally {
			lockHandlers.writeLock().unlock();
		}
	}
	
	/**
	 * @deprecated
	 * Comparing functional interfaces to find the instance to remove is often unreliable<p>
	 * Removes a handler from the list that will be called when this event is posted.<br>
	 * This method behaves like {@link Set#remove(Object)}
	 * @param handler The handler to remove
	 * @return {@code true} when the handler was removed
	 */
	@Deprecated
	public boolean removeHandler(Consumer<E> handler) {
		Objects.requireNonNull(handler, "'handler' Parameter must not be null");
		try {
			lockHandlers.writeLock().lock();
			//We have to find the consumer in the set
			EventHandler<E> found = null;
			for(EventHandler<E> eh : handlers) {
				if(eh.getHandler().equals(handler)) {
					found = eh; //Don't delete it right away, that might be concurrent modification
				}
			}
			if(found == null) {
				return false;
			} else {
				return handlers.remove(found);
			}
		} finally {
			lockHandlers.writeLock().unlock();
		}
	}
	
	/**
	 * Removes all handlers from this event.<br>
	 * This method behaves like {@link Set#clear()}
	 */
	public void clearHandlers() {
		try {
			lockHandlers.writeLock().lock();
			handlers.clear();
		} finally {
			lockHandlers.writeLock().unlock();
		}
	}
	
	/**
	 * The class of the {@link Event} implementation that will be posted to this accessor
	 * @return The class of the event
	 */
	public Class<E> getEventClass() {
		return eventClass;
	}
	
	protected void post(E event) {
		try {
			lockHandlers.readLock().lock();
			for(EventHandler<E> handler : handlers) {
				if(handler.canHandleCancelled() || !event.isCancelled()) { //Not cancelled yet, or if handled anyways
					handler.getHandler().accept(event);
				}
			}
		} finally {
			lockHandlers.readLock().unlock();
		}
	}

	@Override
	public String toString() {
		return "EventAccessor [handlers=" + handlers + ", eventClass=" + eventClass + "]";
	}
}
