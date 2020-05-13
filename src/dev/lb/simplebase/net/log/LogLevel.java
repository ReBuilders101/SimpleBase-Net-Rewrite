package dev.lb.simplebase.net.log;

import dev.lb.simplebase.net.annotation.Immutable;
import dev.lb.simplebase.net.annotation.ValueType;

/**
 * Provides common {@link AbstractLogLevel} implementations.<br>
 * Log level priority values are documented on the enum constants.
 */
@ValueType
@Immutable
public enum LogLevel implements AbstractLogLevel {
	/**
	 * Used for debugging, to mark entering or exiting a method.<br>
	 * Priority value of -20.
	 */
	METHOD(-20),
	
	/**
	 * Used for debugging, to log detailed internal information for troubleshooting.<br>
	 * Priority value of -10.
	 */
	DEBUG(-10),
	
	/**
	 * Used for regular information that should be show to the user, but does not have any special meaning.<br>
	 * Priority value of 0.
	 */
	INFO(0),
	
	/**
	 * Used for warnings to notify the user that something is unusual and may need attention, but does not affect the program otherwise.<br>
	 * Priority value of 10.
	 */
	WARNING(10),
	
	/**
	 * Used for errors that directly affect the user or the stability of the program, but are generally recoverable.<br> 
	 * Priority value of 20.
	 */
	ERROR(20),
	
	/**
	 * Used for fatal errors that the program cannot recover from. Usually causes the program to close, in some cases without proper cleanup.<br>
	 * Priority value of 30.
	 */
	FATAL(30);

	protected static final int LOWEST = METHOD.getPriority();
	protected static final int HIGHEST = FATAL.getPriority();
	
	private final int priority;
	private LogLevel(int priority) {
		this.priority = priority;
	}
	
	@Override
	public int getPriority() {
		return priority;
	}
	
}
