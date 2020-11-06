package dev.lb.simplebase.net.event;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.log.AbstractLogger;

/**
 * An {@link EventDispatcher} posts events to their handlers (stored as {@link EventAccessor}s).
 * This basic implmentation calls all handlers on the same thread that {@link #post(EventAccessor, Event)} is called.
 */
@Threadsafe
public class EventDispatcher {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("event-system");
	
	private final Supplier<String> sourceDescription;
	
	/**
	 * Creates a new synchronous {@link EventDispatcher}.
	 * @param sourceDescription A string description of the object posting to this dispatcher, used for logging only.
	 */
	public EventDispatcher(Supplier<String> sourceDescription) {
		this.sourceDescription = sourceDescription;
	}
	
	/**
	 * Post the event to the handler using this dispatcher.
	 * <p>
	 * The handlers will run on the thread that calls this method. Only one
	 * event can be handled at a time, and any subsequent invocation of this method
	 * from any thread will block until all handlers have completed.
	 * </p>
	 * @param <E> The type of event
	 * @param handler The event accessor storing the handlers
	 * @param event The event instance to post
	 * @return Whether the event was cancelled by a handler 
	 */
	public synchronized <E extends Event> boolean post(EventAccessor<E> handler, E event) {
		Objects.requireNonNull(handler, "'handler' parameter must not be null");
		Objects.requireNonNull(event, "'event' parameter must not be null");
		LOGGER.debug("Posted event (" + handler.getEventClass().getSimpleName() + ") to " +
				handler.getHandlerCount() + " handlers for " + sourceDescription.get());
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
	@Deprecated
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
	@Deprecated
	public synchronized <E extends Event, R> R postAndReturn(EventAccessor<E> handler, E event, R valueIfCancelled, R valueIfNot) {
		post(handler, event);
		if(event.isCancelled()) {
			return valueIfCancelled;
		} else {
			return valueIfNot;
		}
	}
	
	/**
	 * Creates a functional interface that accepts an event instance and passes it to the
	 * specified accessor using this dispatchers {@link #post(EventAccessor, Event)} method.
	 * @param <E> The type of event
	 * @param handler The {@link EventAccessor} that contians the handlers
	 * @return A {@link Consumer} that can be used to post events to that accessor
	 */
	public <E extends Event> Consumer<E> postTask(EventAccessor<E> handler) {
		return (event) -> post(handler, event);
	}
	
	/**
	 * Creates a {@link BooleanSupplier} that will create an event instance using the {@code constructor}
	 * parameter and then will post the event to the {@code target} using this dispatcher.
	 * <p>
	 * The result of the {@link BooleanSupplier#getAsBoolean()} method will be {@code true}
	 * if the posted event has been cancelled by any handlers, and {@code false} otherwise.
	 * @param <E> The type of {@link Event} that will be posted
	 * @param target The {@link EventAccessor} holding the handlers for the event
	 * @param constructor The parameterless constructor for the event instance
	 * @return A {@link BooleanSupplier} that will post the event and report the {@link Event#isCancelled()} state when invoked.
	 */
	public <E extends Event> BooleanSupplier p0Dispatcher(EventAccessor<E> target, Supplier<E> constructor) {
		return () -> post(target, constructor.get());
	}
	
	/**
	 * Creates a {@link Predicate} that will create an event instance using the {@code constructor}
	 * parameter and then will post the event to the {@code target} using this dispatcher.
	 * <p>
	 * The result of the {@link Predicate#test(Object)} method will be {@code true}
	 * if the posted event has been cancelled by any handlers, and {@code false} otherwise.
	 * @param <E> The type of {@link Event} that will be posted
	 * @param <T> The type of the event constructor's parameter
	 * @param target The {@link EventAccessor} holding the handlers for the event
	 * @param constructor The constructor for the event instance taking one parameter of type {@code T}
	 * @return A {@link Predicate} that will post the event and report the {@link Event#isCancelled()} state when invoked.
	 */
	public <T, E extends Event> Predicate<T> p1Dispatcher(EventAccessor<E> target, Function<T, E> constructor) {
		return (t) -> post(target, constructor.apply(t));
	}
	
	/**
	 * Creates a {@link BiPredicate} that will create an event instance using the {@code constructor}
	 * parameter and then will post the event to the {@code target} using this dispatcher.
	 * <p>
	 * The result of the {@link BiPredicate#test(Object, Object)} method will be {@code true}
	 * if the posted event has been cancelled by any handlers, and {@code false} otherwise.
	 * @param <E> The type of {@link Event} that will be posted
	 * @param <T1> The type of the event constructor's first parameter
	 * @param <T2> The type of the event constructor's second parameter
	 * @param target The {@link EventAccessor} holding the handlers for the event
	 * @param constructor The constructor for the event instance taking two parameters of type {@code T1} and {@code T2}
	 * @return A {@link BiPredicate} that will post the event and report the {@link Event#isCancelled()} state when invoked.
	 */
	public <T1, T2, E extends Event> BiPredicate<T1, T2> p2Dispatcher(EventAccessor<E> target, BiFunction<T1, T2, E> constructor) {
		return (t1, t2) -> post(target, constructor.apply(t1, t2));
	}
	
	/**
	 * A dispatcher implementation that discards all posted events silently.
	 * @return An empty {@link EventDispatcher}
	 */
	public static EventDispatcher emptyDispatcher() {
		return new EventDispatcher(() -> "") {

			@Override
			public synchronized <E extends Event> boolean post(EventAccessor<E> handler, E event) {
				return false;
			}

			@Override
			public synchronized <E extends Event> void postAndRun(EventAccessor<E> handler, E event,
					Runnable taskIfCancelled, Runnable taskIfNot) {}

			@Override
			public synchronized <E extends Event, R> R postAndReturn(EventAccessor<E> handler, E event,
					R valueIfCancelled, R valueIfNot) {
				return valueIfNot;
			}

			@Override
			public <E extends Event> Consumer<E> postTask(EventAccessor<E> handler) {
				return (e) -> {};
			}
			
		};
	}
	
}
