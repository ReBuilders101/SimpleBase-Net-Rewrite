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
	 * The highest possible log level. Everything will be logged when this is used as a minimum level.<br>
	 * Priority value of {@link Integer#MIN_VALUE}
	 */
	LOWEST(Integer.MIN_VALUE),
	
	/**
	 * Used for debugging, to mark entering or exiting a method.<br>
	 * Priority value of 500.
	 */
	METHOD(500),
	
	/**
	 * Used for debugging, to log detailed internal information for troubleshooting.<br>
	 * Priority value of 700.
	 */
	DEBUG(700),
	
	/**
	 * Used for regular information that should be show to the user, but does not have any special meaning.<br>
	 * Priority value of 800.
	 */
	INFO(800),
	
	/**
	 * Used for warnings to notify the user that something is unusual and may need attention, but does not affect the program otherwise.<br>
	 * Priority value of 900.
	 */
	WARNING(900),
	
	/**
	 * Used for errors that directly affect the user or the stability of the program, but are generally recoverable.<br> 
	 * Priority value of 1000.
	 */
	ERROR(1000),
	
	/**
	 * Used for fatal errors that the program cannot recover from. Usually causes the program to close, in some cases without proper cleanup.<br>
	 * Priority value of 1100.
	 */
	FATAL(1100),
	
	/**
	 * The highest possible log level. Nothing will be logged when this is used as a minimum level (except if posted with this level).<br>
	 * Priority value of {@link Integer#MAX_VALUE}
	 */
	HIGHEST(Integer.MAX_VALUE);

	
	private final int priority;
	private LogLevel(int priority) {
		this.priority = priority;
	}
	
	@Override
	public int getPriority() {
		return priority;
	}
	
}
