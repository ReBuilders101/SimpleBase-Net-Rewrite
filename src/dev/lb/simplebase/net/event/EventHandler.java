package dev.lb.simplebase.net.event;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a method as an event handler method that can be detected by {@link EventAccessor#addAllHandlers(Class, EventAccessor...)}.
 * <p>
 * The following conditions have to apply to make a method an event handler:
 * <ul>
 * <li>It must be {@code public}</li>
 * <li>It must be {@code static}</li>
 * <li>It must not have a Varargs parameter</li>
 * <li>It must have exactly one parameter</li>
 * <li>It must not throw any checked exceptions</li>
 * <li>It must not have any type parameters</li>
 * <li>The single parameter must be a subtype of {@link Event}</li>
 * <li>The method must have this annotation</li>
 * </ul>
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface EventHandler {
	/**
	 * The {@link EventHandlerPriority} that the handler represented by this method should have
	 */
	public EventHandlerPriority priority() default EventHandlerPriority.NORMAL;
	
	/**
	 * If {@code true}, the handler for this method will receive cancelled events
	 */
	public boolean receiveCancelled() default false;
}
