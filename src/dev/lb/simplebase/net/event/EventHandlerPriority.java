package dev.lb.simplebase.net.event;

/**
 * The priority of an event handler. Higher priorities handle events before lower priorities.
 */
public enum EventHandlerPriority {
	
	/**
	 * Runs last. If more than one handler is registered with this
	 * priority, they can run in any order. For this reason, this priority should
	 * be avoided unless absolutely necessary.
	 */
	LOWEST,
	/**
	 * Runs after the {@link #NORMAL} priority, but before {@link #LOWEST}.
	 */
	LOW, 
	/**
	 * The default event handler priority.
	 * It is recommended to use this if no special requirements for handling
	 * order exist.
	 */
	NORMAL, 
	/**
	 * Runs after {@link #HIGHEST}, but before {@link #NORMAL}.
	 */
	HIGH, 
	/**
	 * Runs first. If more than one handler is registered with this
	 * priority, they can run in any order. For this reason, this priority should
	 * be avoided unless absolutely necessary.
	 */
	HIGHEST;
	
}
