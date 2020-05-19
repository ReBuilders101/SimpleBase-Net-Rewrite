package dev.lb.simplebase.net.event;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
	private final Set<AbstractEventHandler<E>> handlers;
	private final Class<E> eventClass;
	
	public EventAccessor(Class<E> eventClass) {
		Objects.requireNonNull(eventClass, "'eventClass' Parameter must not be null");
		this.eventClass = eventClass;
		this.lockHandlers = new ReentrantReadWriteLock();
		this.handlers = new TreeSet<>(); //Use the natural order to sort by priority
	}
	
	protected boolean addHandler(AbstractEventHandler<E> handler) {
		try {
			lockHandlers.writeLock().lock();
			return handlers.add(handler);
		} finally {
			lockHandlers.writeLock().unlock();
		}
	}
	
	/**
	 * Adds a handler that will be called when this event is posted.<br>
	 * This method behaves like {@link Set#add(Object)}
	 * @param handler The new handler
	 * @param priority The {@link EventHandlerPriority} of the handler
	 * @param receiveCancelled If {@code true}, the handler will also be called for cancelled events
	 * @return {@code true} when the handler was added
	 */
	public boolean addHandler(Consumer<E> handler, EventHandlerPriority priority, boolean receiveCancelled) {
		Objects.requireNonNull(handler, "'handler' Parameter must not be null");
		Objects.requireNonNull(priority, "'priority' Parameter must not be null");
		Objects.requireNonNull(receiveCancelled, "'receiveCancelled' Parameter must not be null");
		return addHandler(new FunctionalEventHandler<>(handler, priority, receiveCancelled));
	}
	
	/**
	 * Adds a handler that will be called when this event is posted. It will not receive cancelled events.<br>
	 * This method behaves like {@link Set#add(Object)}
	 * @param handler The new handler
	 * @param priority The {@link EventHandlerPriority} of the handler
	 * @return {@code true} when the handler was added
	 */
	public boolean addHandler(Consumer<E> handler, EventHandlerPriority priority) {
		return addHandler(handler, priority, false);
	}
	
	/**
	 * Adds a handler that will be called when this event is posted. It will have {@code NORMAL} priority.<br>
	 * This method behaves like {@link Set#add(Object)}
	 * @param handler The new handler
	 * @param receiveCancelled If {@code true}, the handler will also be called for cancelled events
	 * @return {@code true} when the handler was added
	 */
	public boolean addHandler(Consumer<E> handler, boolean receiveCancelled) {
		return addHandler(handler, EventHandlerPriority.NORMAL, receiveCancelled);
	}
	
	/**
	 * Adds a handler that will be called when this event is posted. 
	 * It will not receive cancelled events and have {@code NORMAL} priority.<br>
	 * This method behaves like {@link Set#add(Object)}
	 * @param handler The new handler
	 * @return {@code true} when the handler was added
	 */
	public boolean addHandler(Consumer<E> handler) {
		return addHandler(handler, EventHandlerPriority.NORMAL, false);
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
	
	/**
	 * The amount of handlers registered with this {@link EventAccessor}.
	 * @return The amount of handlers
	 */
	public int getHandlerCount() {
		try {
			lockHandlers.readLock().lock();
			return handlers.size();
		} finally {
			lockHandlers.readLock().unlock();
		}
	}
	
	protected void post(E event) {
		try {
			lockHandlers.readLock().lock();
			for(AbstractEventHandler<E> handler : handlers) {
				if(handler.canHandleCancelled() || !event.isCancelled()) { //Not cancelled yet, or if handled anyways
					handler.runHandler(event);
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
	
	
	
	public static void addAllHandlers(Class<?> containerType, EventAccessor<?>...eventAccessors) {
		final Map<Class<?>, EventAccessor<?>> eventAccessorMap = 
				Arrays.stream(eventAccessors).collect(Collectors.toMap(EventAccessor::getEventClass, Function.identity(),
				(a, b) -> { throw new IllegalArgumentException("Two EventAccessors for the same type: " + a.getEventClass().getCanonicalName()); }));
		
		final Method[] methods = containerType.getDeclaredMethods(); //Don't include superclass ones
		for(Method method : methods) {
			final int mods = method.getModifiers();
			//method must be public static, no varargs, 1 param
			if(Modifier.isStatic(mods) && Modifier.isPublic(mods) &&
					!method.isVarArgs() && method.getParameterCount() == 1) {
				final Class<?>[] params = method.getParameterTypes();
				//Must have 1 param that is a subclass of Event, no exceptions or generics
				if(params.length == 1 && params[0] != null &&
						method.getExceptionTypes().length == 0 &&
						method.getParameterTypes().length == 0 &&
						Event.class.isAssignableFrom(params[0])) {
					//Must have the annotation
					if(method.isAnnotationPresent(EventHandler.class)) {
						final EventHandler annotation = method.getAnnotation(EventHandler.class);
						//It is valid, now find the accessor
						final EventAccessor<?> accessor = eventAccessorMap.get(params[0]);
						if(accessor != null) {
							accessor.addHandler(new ReflectionEventHandler<>(method, annotation.priority(), annotation.receiveCancelled()));
						}
					}
				}
			}
		}
		
	}
	
}
