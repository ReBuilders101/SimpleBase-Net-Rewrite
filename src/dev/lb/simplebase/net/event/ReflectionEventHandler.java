package dev.lb.simplebase.net.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionEventHandler<E extends Event> extends AbstractEventHandler<E> {
	
	private final Method methodHandler;
	
	protected ReflectionEventHandler(Method method, EventHandlerPriority priority, boolean handleCancelled) {
		super(priority, handleCancelled);
		this.methodHandler = method;
	}

	@Override
	public void runHandler(E event) {
		try {
			methodHandler.invoke(null, event);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			EventDispatcher.LOGGER.error("Exception while calling reflection event handler:", e);
		}
	}

}
