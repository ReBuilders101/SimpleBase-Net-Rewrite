package dev.lb.simplebase.net.log;

import dev.lb.simplebase.net.annotation.Immutable;
import dev.lb.simplebase.net.annotation.ValueType;
import dev.lb.simplebase.net.log.LogLevel.CustomLogLevel;

/**
 * Describes a log level that can have any priority.
 * AbstractLogLevels are sorted by an integer value available with {@link #getPriority()}
 * to sort them and check which one has a higher log level.<br>
 * The {@link LogLevel} enum provides common log levels, the {@link #create(int)} method can be used
 * to create any other level.
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
	
	/**
	 * Creates a new log level from any priority number.<br>
	 * {@link AbstractLogLevel}s are {@link ValueType}s, so this method may return an
	 * already existing instance, or a completely new one. The returned implementation class is not determined.<br>
	 * The only thing this method promises is that the instance will return the correct value when
	 * calling its {@link #getPriority()} method, and that two calls with the same parameter value will return
	 * instances that are equal by their {@link #equals(Object)} method.
	 * @param priority The priority for the custom log level
	 * @return A custom or preexisting {@link AbstractLogLevel}
	 */
	public static AbstractLogLevel create(int priority) {
		//Find out whether the priority corresponds to an existing LogLevel constant
		for(LogLevel existing : LogLevel.values()) {
			if(existing.getPriority() == priority) return existing;
		}
		return new CustomLogLevel(priority); //Custom one required
	}
	
	/**
	 * Creates a new log level from any priority number.<br>
	 * {@link AbstractLogLevel}s are {@link ValueType}s, so this method may return an
	 * already existing instance, or a completely new one. The returned implementation class is not determined.<br>
	 * The only thing this method promises is that the instance will return the correct value when
	 * calling its {@link #getPriority()} method, and that two calls with the same parameter value will return
	 * instances that are equal by their {@link #equals(Object)} method.
	 * <p>
	 * Searches the AbstractLogLevels in the search space parameter (in addition to the {@link LogLevel} enum constants)
	 * for matching instances to return
	 * @param additionalSearchSpace The instances to search for a match
	 * @param priority The priority for the custom log level
	 * @return A custom or preexisting {@link AbstractLogLevel}
	 */
	public static AbstractLogLevel create(int priority, AbstractLogLevel...additionalSearchSpace) {
		//Find out whether the priority corresponds to an existing LogLevel constant
		for(LogLevel existing : LogLevel.values()) {
			if(existing.getPriority() == priority) return existing;
		}
		for(AbstractLogLevel existing : additionalSearchSpace) {
			if(existing.getPriority() == priority) return existing;
		}
		return new CustomLogLevel(priority); //Custom one required
	}
}
