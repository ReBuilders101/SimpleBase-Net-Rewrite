package dev.lb.simplebase.net.log;

import dev.lb.simplebase.net.annotation.Immutable;
import dev.lb.simplebase.net.annotation.ValueType;

/**
 * Describes a log level that can have any priority.
 * AbstractLogLevels are sorted by an integer value available with {@link #getPriority()}
 * to sort them and check which one has a higher log level.<br>
 * The {@link LogLevel} enum provides common log levels, the {@link CustomLogLevel} implementation can be used
 * to create any other level with {@link CustomLogLevel#create(int)}.
 * <p><b>Should not be implemented by API users.</b> The {@code #equals(Object)} method of an implementing class must have
 * special behavior so that instances can be equal to any other implementation with the same priority.</p>
 */
@ValueType
@Immutable
public interface AbstractLogLevel {

	/**
	 * The log level priority. Higher levels (e.g. {@link LogLevel#ERROR}) have a higher
	 * priority than lower levels (e.g. {@link LogLevel#DEBUG}). Many {@link Logger} implementations
	 * have a lower cutoff point (or minimum log level), where messages with a lower level (and lower priority value)
	 * are no longer displayed.
	 * @return The numerical priority of this log level
	 */
	public int getPriority();
	
	/**
	 * Checks whether this log level is above a certain lower cutoff point.<br>
	 * If this instances priority is higher than ore equal to the parameters priority,
	 * this method returns {@code true}, otherwise it returns {@code false}<br>
	 * Can be used by {@link Logger} implementations to decide whether to display
	 * a message considering its minimal logging level.
	 * @param lowerCutoff The minimal log level
	 * @return Whether this log level has higher or equal priority than the other one
	 */
	public default boolean isAbove(AbstractLogLevel lowerCutoff) {
		return getPriority() >= lowerCutoff.getPriority();
	}
	
}
