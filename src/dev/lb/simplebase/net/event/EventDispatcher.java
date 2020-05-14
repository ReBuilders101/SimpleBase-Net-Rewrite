package dev.lb.simplebase.net.event;

import java.util.Objects;
import java.util.function.Consumer;

import dev.lb.simplebase.net.annotation.Threadsafe;

/**
 * An {@link EventDispatcher} posts events to their handlers (stored as {@link EventAccessor}s).
 * This basic implmentation calls all handlers on the same thread that {@link #post(EventAccessor, Event)} is called.
 */
@Threadsafe
public class EventDispatcher {

	/**
	 * Creates a new {@link EventDispatcher}.
	 */
	public EventDispatcher() {}
	
	/**
	 * Post the event to the handler using this dispatcher.
	 * The exact way the handlers are executed depends on the dispatcher implementation
	 * <p>
	 * the method is synchronized to ensure that the
	 * @param <E> The type of event
	 * @param handler The event handler(s)
	 * @param event The event to post
	 */
	public synchronized <E extends Event> void post(EventAccessor<E> handler, E event) {
		Objects.requireNonNull(handler, "'handler' parameter must not be null");
		Objects.requireNonNull(event, "'event' parameter must not be null");
		handler.post(event);
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
