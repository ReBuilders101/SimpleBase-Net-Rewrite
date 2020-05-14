package dev.lb.simplebase.net.log;

import java.util.function.Supplier;

/**
 * Logs data and stuff.
 * Create using the {@link Loggers} class.
 */
public interface AbstractLogger {
//Loggers are generally pretty self-explanatory, so no doc for all of these methods
	
	public void enterMethod(String comment);
	
	public void exitMethod(String comment);
	
	public default void debug(String message) {
		log(LogLevel.DEBUG, message);
	}
	
	public default void debug(Supplier<String> message) {
		log(LogLevel.DEBUG, message);
	}
	
	public default void debug(String formatString, Object...objects) {
		log(LogLevel.DEBUG, formatString, objects);
	}
	
	
	public default void info(String message) {
		log(LogLevel.INFO, message);
	}
	
	public default void info(Supplier<String> message) {
		log(LogLevel.INFO, message);
	}
	
	public default void info(String formatString, Object...objects) {
		log(LogLevel.INFO, formatString, objects);
	}
	
	
	public default void warning(String message) {
		log(LogLevel.WARNING, message);
	}
	
	public default void warning(Supplier<String> message) {
		log(LogLevel.WARNING, message);
	}
	
	public default void warning(String formatString, Object...objects) {
		log(LogLevel.WARNING, formatString, objects);
	}
	
	public default void warning(Exception messageAndStacktrace) {
		log(LogLevel.WARNING, messageAndStacktrace);
	}
	
	
	public default void error(String message) {
		log(LogLevel.ERROR, message);
	}
	
	public default void error(Supplier<String> message) {
		log(LogLevel.ERROR, message);
	}
	
	public default void error(String formatString, Object...objects) {
		log(LogLevel.ERROR, formatString, objects);
	}
	
	public default void error(Exception messageAndStacktrace) {
		log(LogLevel.ERROR, messageAndStacktrace);
	}
	
	
	public default void fatal(String message) {
		log(LogLevel.FATAL, message);
	}
	
	public default void fatal(Supplier<String> message) {
		log(LogLevel.FATAL, message);
	}
	
	public default void fatal(String formatString, Object...objects) {
		log(LogLevel.FATAL, formatString, objects);
	}
	
	public default void fatal(Exception messageAndStacktrace) {
		log(LogLevel.FATAL, messageAndStacktrace);
	}
	
	
	public void log(AbstractLogLevel level, String message);
	
	public void log(AbstractLogLevel level, Supplier<String> message);
	
	public void log(AbstractLogLevel level, String formatString, Object...objects);

	public void log(AbstractLogLevel level, Exception messageAndStacktrace);
	
	
	//Don't default/chain these as stack depth is important
	public void stack(AbstractLogLevel level);
	
	public void stack(AbstractLogLevel level, int popEntries);
	
	public void stack(AbstractLogLevel level, String comment);
	
	public void stack(AbstractLogLevel level, int popEntries, String comment);
	
	
	public AbstractLogLevel getLogLevel();
	
	public void setLogLevel(AbstractLogLevel logLevel);
}
