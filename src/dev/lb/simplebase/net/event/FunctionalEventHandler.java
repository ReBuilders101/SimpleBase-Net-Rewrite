package dev.lb.simplebase.net.event;

import java.util.function.Consumer;

import dev.lb.simplebase.net.annotation.Internal;

@Internal
class FunctionalEventHandler<E extends Event> extends AbstractEventHandler<E> {

	private final Consumer<E> handler;
	
	protected FunctionalEventHandler(Consumer<E> handler, EventHandlerPriority priority, boolean handleCancelled) {
		super(priority, handleCancelled);
		this.handler = handler;
	}

	@Override
	public void runHandler(E event) {
		handler.accept(event);
	}

}
