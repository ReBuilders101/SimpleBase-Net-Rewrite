package dev.lb.simplebase.net.log;

import java.util.Objects;

import dev.lb.simplebase.net.NetworkManager;
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
	 * @param priority
	 * @return
	 */
	public static AbstractLogLevel create(int priority) {
		//Find out whether the priority corresponds to an existing LogLevel constant
		if(priority < LogLevel.LOWEST ||
		   priority > LogLevel.HIGHEST ||
		   priority % 10 != 0) { //Out of range or not divisible by 10 ->
			return new CustomLogLevel(priority); //Custom one required
		} else {
			final int index = priority - LogLevel.LOWEST / 10; //Calculate the index where the Value would be found
			try {
				AbstractLogLevel level = LogLevel.values()[index]; //Access the index. OutOfBounds will be caught
				if(level.getPriority() == priority) {  //Check that we really have the right one
					return level;
				} else { //Otherwise complain, and create a new LogLevel
					NetworkManager.NET_LOG.error("Inconsistent enum declaration in LogLevel at index %d for priority %d: Wrong priority value for enum constant %s",
							index, priority, level);
					return new CustomLogLevel(priority);
				}
			} catch (ArrayIndexOutOfBoundsException e) { //If our array index was out of bounds, something was wrong with the enum
				NetworkManager.NET_LOG.error("Inconsistent enum declaration in LogLevel at index %d for priority %d: Out of bounds", index, priority);
				return new CustomLogLevel(priority); //Return something useful anyways
			}
		}
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