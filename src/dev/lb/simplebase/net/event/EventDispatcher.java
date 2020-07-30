package dev.lb.simplebase.net.event;

import java.util.Objects;
import java.util.function.Consumer;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.NetworkManagerCommon;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.log.AbstractLogger;

/**
 * An {@link EventDispatcher} posts events to their handlers (stored as {@link EventAccessor}s).
 * This basic implmentation calls all handlers on the same thread that {@link #post(EventAccessor, Event)} is called.
 */
@Threadsafe
public class EventDispatcher {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("event-system");
	
	private final NetworkManagerCommon manager;
	
	/**
	 * Creates a new {@link EventDispatcher}.
	 * @param manager The network manager that generates the posted events
	 */
	public EventDispatcher(NetworkManagerCommon manager) {
		this.manager = manager;
	}
	
	/**
	 * Post the event to the handler using this dispatcher.
	 * The exact way the handlers are executed depends on the dispatcher implementation
	 * <p>
	 * the method is synchronized to ensure that the
	 * @param <E> The type of event
	 * @param handler The event handler(s)
	 * @param event The event to post
	 * @return Whether the event was cancelled 
	 */
	public synchronized <E extends Event> boolean post(EventAccessor<E> handler, E event) {
		Objects.requireNonNull(handler, "'handler' parameter must not be null");
		Objects.requireNonNull(event, "'event' parameter must not be null");
		LOGGER.debug("Posted event (" + handler.getEventClass() + ") to " +
				handler.getHandlerCount() + " handlers for " + manager.getLocalID().getDescription());
		handler.post(event);
		return event.isCancelled();
	}
	

	/**
	 * Post the event to the handler using this dispatcher.
	 * The exact way the handlers are executed depends on the dispatcher implementation
	 * <p>
	 * the method is synchronized to ensure that the
	 * @param <E> The type of event
	 * @param handler The event handler(s)
	 * @param event The event to post
	 * @param taskIfCancelled The task to run if the event is canecelled
	 * @param taskIfNot The task to run if the event is not canecelled
	 */
	public synchronized <E extends Event> void postAndRun(EventAccessor<E> handler, E event, Runnable taskIfCancelled, Runnable taskIfNot) {
		post(handler, event);
		if(event.isCancelled()) {
			taskIfCancelled.run();
		} else {
			taskIfNot.run();
		}
	}
	
	/**
	 * Post the event to the handler using this dispatcher.
	 * The exact way the handlers are executed depends on the dispatcher implementation
	 * <p>
	 * the method is synchronized to ensure that the
	 * @param <E> The type of event
	 * @param <R> The return type
	 * @param handler The event handler(s)
	 * @param event The event to post
	 * @param valueIfCancelled The value to return if the event is canecelled
	 * @param valueIfNot The value to return if the event is not canecelled
	 * @return The value depending on the cancel flag
	 */
	public synchronized <E extends Event, R> R postAndReturn(EventAccessor<E> handler, E event, R valueIfCancelled, R valueIfNot) {
		post(handler, event);
		if(event.isCancelled()) {
			return valueIfCancelled;
		} else {
			return valueIfNot;
		}
	}
	
	/**
	 * Creates an event handler that accepts an event instance and passes it to the
	 * specified accessor using this dispatchers {@link #post(EventAccessor, Event)} method.
	 * @param <E> The type of event
	 * @param handler The {@link EventAccessor} that contians the handlers
	 * @return A {@link Consumer} that can be used to post events to that accessor
	 */
	public <E extends Event> Consumer<E> postTask(EventAccessor<E> handler) {
		return (event) -> post(handler, event);
	}
	
}
