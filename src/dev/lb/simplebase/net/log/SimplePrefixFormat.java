package dev.lb.simplebase.net.log;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import dev.lb.simplebase.net.log.Logger.LoggerDelegate;

/**
 * A {@link SimplePrefixFormat} implementation can generate a prefix for a log message
 */
public interface SimplePrefixFormat {

	/**
	 * Return a prefix for a logged message.
	 * <p>
	 * If the prefix is composed of more than one string, they
	 * should be returned as elements of the stream so they can be combined
	 * with elements from all other prefixes in a single string builder.
	 * </p>
	 * @param level The {@link AbstractLogLevel} of the logged message
	 * @return A {@link Stream} consisting of all elements in the prefix
	 */
	public Stream<String> getPrefix(AbstractLogLevel level);
	
	
	/**
	 * A {@link SimplePrefixFormat} that prints the log level of the message.
	 * <p>
	 * Prints <pre>[&lt;loglevel&gt;]</pre>
	 * {@code <loglevel>} will be replaced with the matching name of
	 * the enum constant in {@link LogLevel}. If the log level was created with {@link AbstractLogLevel#create(int)}
	 * and has no matching enum constant, it will be printed as 
	 * <pre>[LogLevel:&lt;priority&gt;]</pre>{@code <priority>} is
	 * the number returned by {@link AbstractLogLevel#getPriority()}.
	 * </p>
	 * @return A {@link SimplePrefixFormat} that prints the log level of the message
	 */
	public static SimplePrefixFormat forLogLevel() {
		return level -> Stream.of("[", level.toString(), "]");
	}
	
	/**
	 * A {@link SimplePrefixFormat} that prints the name of the current thread.
	 * <p>
	 * Prints <pre>[&lt;threadname&gt;]</pre>{@code <threadname>} will be replaced with the
	 * name of the calling thread.
	 * </p><p>
	 * This formatter might not work correctly if the {@link LoggerDelegate} handles logging on
	 * a different thread from the one where the log method was called.
	 * </p>
	 * @return A {@link SimplePrefixFormat} that prints the name of the current thread
	 */
	public static SimplePrefixFormat forThread() {
		return level -> Stream.of("[", Thread.currentThread().getName(), "]");
	}
	
	/**
	 * A {@link SimplePrefixFormat} that prints the current time and/or date.
	 * <p>
	 * Prints <pre>[&lt;formattedtime&gt;]</pre>{@code <formattedtime>} will be replaced with the
	 * result of the {@link DateTimeFormatter}. The {@link LocalDateTime#now()} method is used to determine
	 * the date/time that should be logged
	 * </p>
	 * @param format The {@link DateTimeFormatter} used to convert the {@link LocalDateTime} representing the date/time to
	 * log into a {@link String} value
	 * @return A {@link SimplePrefixFormat} that prints the current time and/or date
	 */
	public static SimplePrefixFormat forTime(DateTimeFormatter format) {
		return level -> Stream.of("[", format.format(LocalDateTime.now()), "]");
	}
	
	/**
	 * A {@link SimplePrefixFormat} that prints a static {@link String} value.
	 * <p>
	 * Prints <pre>[&lt;string&gt;]</pre>{@code <string>} will be replaced with the
	 * value of the {@code text} parameter
	 * </p>
	 * @param text The {@link String} to print as a prefix
	 * @return A {@link SimplePrefixFormat} that prints the a static value
	 */
	public static SimplePrefixFormat forString(String text) {
		return level -> Stream.of("[", text, "]");
	}
}
