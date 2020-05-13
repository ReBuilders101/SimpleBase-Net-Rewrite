package dev.lb.simplebase.net.log;

import java.util.Objects;

import dev.lb.simplebase.net.annotation.Immutable;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.ValueType;

/**
 * Provides the static {@link #create(int)} method to crete {@link AbstractLogLevel}s of any priority.
 */
@ValueType
@Immutable
public class CustomLogLevel implements AbstractLogLevel {
	
	private final int priority;
	
	@Internal
	private CustomLogLevel(int priority) {
		this.priority = priority;
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

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public String toString() {
		return "CustomLogLevel [priority=" + priority + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(priority);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		//Adjusted the equals method so a CustomLogLevel can be equal to a LogLevel enum
		if (!(obj instanceof AbstractLogLevel)) {
			return false;
		}
		AbstractLogLevel other = (AbstractLogLevel) obj;
		return priority == other.getPriority();
	}
	
}