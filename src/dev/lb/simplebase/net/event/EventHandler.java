package dev.lb.simplebase.net.event;

import java.util.function.Consumer;

import dev.lb.simplebase.net.annotation.Immutable;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.ValueType;

@Internal
@ValueType
@Immutable
class EventHandler<E extends Event> implements Comparable<EventHandler<?>> { //Comparable to any other handler

	private final Consumer<E> handler;
	private final EventHandlerPriority priority;
	private final boolean handleCancelled;
	
	/**
	 * No null values allowed!
	 */
	protected EventHandler(Consumer<E> handler, EventHandlerPriority priority, boolean handleCancelled) {
		this.handler = handler;
		this.priority = priority;
		this.handleCancelled = handleCancelled;
	}

	public Consumer<E> getHandler() {
		return handler;
	}

	public EventHandlerPriority getPriority() {
		return priority;
	}

	public boolean canHandleCancelled() {
		return handleCancelled;
	}

	@Override
	public int compareTo(EventHandler<? extends Event> o) {
		return priority.compareTo(o.priority);
	}
	
}
