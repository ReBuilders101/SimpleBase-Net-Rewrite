package dev.lb.simplebase.net.log;

import java.io.PrintStream;
import java.util.Objects;

import dev.lb.simplebase.net.annotation.StaticType;

/**
 * Provides static methods to create {@link AbstractLogger} instances.
 */
@StaticType
public class Loggers {

	private Loggers() {/*No instances possibe*/}
	
	/**
	 * Creates an empty Logger implementation that discards any logged message
	 * @return The empty logger
	 */
	public static AbstractLogger empty() {
		return new EmptyLogger();
	}
	
	/**
	 * Creates a Logger implementation that prints logs to a {@link PrintStream}.
	 * @param stream The {@link PrintStream} to use for logging
	 * @param minimumLevel The minimal logging level that a message needs to be logged
	 * @return The logger for this print stream
	 */
	public static AbstractLogger printStream(PrintStream stream, AbstractLogLevel minimumLevel) {
		return printStream(stream, minimumLevel, Formatter.getDefault());
	}
	
	/**
	 * Creates a Logger implementation that prints logs to a {@link PrintStream}.
	 * @param stream The {@link PrintStream} to use for logging
	 * @param minimumLevel The minimal logging level that a message needs to be logged
	 * @param formatter The {@link Formatter} used to generate the log output
	 * @return The logger for this print stream
	 */
	public static AbstractLogger printStream(PrintStream stream, AbstractLogLevel minimumLevel, Formatter formatter) {
		Objects.requireNonNull(stream, "'stream' parameter must not be null");
		Objects.requireNonNull(minimumLevel, "'minimumLevel' parameter must not be null");
		Objects.requireNonNull(formatter, "'formatter' parameter must not be null");
		return new PrintStreamLogger(minimumLevel, true, formatter, stream);
	}
	
	/**
	 * Creates a Logger implementation that prints logs to {@link System#out}
	 * @param minimumLevel The minimal logging level that a message needs to be logged
	 * @return The logger for this print stream
	 */
	public static AbstractLogger printSysOut(AbstractLogLevel minimumLevel) {
		return printSysOut(minimumLevel, Formatter.getDefault());
	}
	
	/**
	 * Creates a Logger implementation that prints logs to {@link System#out}
	 * @param minimumLevel The minimal logging level that a message needs to be logged
	 * @param formatter The {@link Formatter} used to generate the log output
	 * @return The logger for this print stream
	 */
	public static AbstractLogger printSysOut(AbstractLogLevel minimumLevel, Formatter formatter) {
		Objects.requireNonNull(minimumLevel, "'minimumLevel' parameter must not be null");
		Objects.requireNonNull(formatter, "'formatter' parameter must not be null");
		return new PrintStreamLogger(minimumLevel, true, formatter, System.out);
	}
	
}
