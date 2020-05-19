package dev.lb.simplebase.net.event;

import dev.lb.simplebase.net.annotation.Immutable;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.ValueType;

@Internal
@ValueType
@Immutable
abstract class AbstractEventHandler<E extends Event> implements Comparable<AbstractEventHandler<?>> { //Comparable to any other handler

	private final EventHandlerPriority priority;
	private final boolean handleCancelled;
	
	/**
	 * No null values allowed!
	 */
	protected AbstractEventHandler(EventHandlerPriority priority, boolean handleCancelled) {
		this.priority = priority;
		this.handleCancelled = handleCancelled;
	}
	
	public abstract void runHandler(E event);

	public EventHandlerPriority getPriority() {
		return priority;
	}

	public boolean canHandleCancelled() {
		return handleCancelled;
	}

	@Override
	public int compareTo(AbstractEventHandler<? extends Event> o) {
		return priority.compareTo(o.priority);
	}
	
}
