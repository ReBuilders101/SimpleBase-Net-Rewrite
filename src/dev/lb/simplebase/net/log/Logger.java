package dev.lb.simplebase.net.log;

import java.util.Formatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import dev.lb.simplebase.net.annotation.Internal;

/**
 * <p>
 * Base class for any type of logger. Can delegate to a number of logging Frameworks based on the
 * implementation chosen at creation time.
 * </p><p>
 * Stores a lowest log level ({@link #getLogLevel()}). Only messages above this cutoff will be passed to the delegate.
 * </p>
 */
public final class Logger {
	
	/**
	 * A {@link LoggerDelegate} represents an implementation used by the {@link Logger} class.
	 */
	public static interface LoggerDelegate {
		
		/**
		 * Logs a method entry/exit message at {@link LogLevel#METHOD}.
		 * @param enter If {@code true} log entry, if {@code false} log exit
		 * @param comment Optional additional message. May be {@code null}
		 * @param stackPop Amount of stack frames to pop to get the method to log
		 */
		public default void methodImpl(boolean enter, String comment, int stackPop) {
			//get the entire stack
			final StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();
			//We have to access currentStack[stackPop]
			if(stackPop > currentStack.length - 1) {
				throw new IllegalArgumentException("Cannot get stack element " + stackPop + " for logging");
			}
			
			final StackTraceElement callingMethodElement = currentStack[stackPop];
			final StringBuilder message = new StringBuilder(enter ? "Entered" : "Exited");
			message.append(" method: ").append(callingMethodElement);
			if(comment != null && !comment.isEmpty()) {
				message.append(" (").append(comment).append(")");
			}
			
			logImpl(LogLevel.METHOD, message.toString(), null);
		}
		
		/**
		 * Logs a stack trace.
		 * @param level The {@link AbstractLogLevel} for the stack trace
		 * @param stackPop The amount of stack frames to pop before logging
		 * @param comment Optional additional message. May be {@code null}
		 */
		public default void stackImpl(AbstractLogLevel level, int stackPop, String comment) {
			final StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();
			if(stackPop > currentStack.length - 1) {
				throw new IllegalArgumentException("Cannot get stack element " + stackPop + " for logging");
			}
			
			final StackTraceElement[] partialStack = new StackTraceElement[currentStack.length - stackPop];
			System.arraycopy(currentStack, stackPop, partialStack, 0, currentStack.length - stackPop);
			final StringBuilder message = new StringBuilder("Stacktrace");
			if(comment != null && !comment.isEmpty()) {
				message.append(" (").append(comment).append(")");
			}
			message.append(":");
			
			//Stack trace with leading line breaks and tab
			for(StackTraceElement se : partialStack) {
				message.append("\n	").append(se);
			}
			
			logImpl(level, message.toString(), null);
		}
		
		/**
		 * Logs a message.
		 * @param level The {@link AbstractLogLevel} for the message
		 * @param message The raw message to log. May be {@code null} <b>only</b> if {@code exception} is not
		 * @param stacktrace An exception to log with message and stacktrace. May be {@code null}
		 */
		public void logImpl(AbstractLogLevel level, String message, Exception stacktrace);
	}
	
	@Internal
	Logger(LoggerDelegate delegate, AtomicReference<AbstractLogLevel> level) {
		this.delegate = delegate;
		this.cutoffLevel = level;
	}
	
	final LoggerDelegate delegate;
	final AtomicReference<AbstractLogLevel> cutoffLevel;
	
	private boolean canLog(AbstractLogLevel level) {
		return Objects.requireNonNull(level, "log level must not be null").isAbove(cutoffLevel.get());
	}
	
	/**
	 * Logs a message at {@link LogLevel#METHOD} that the calling method has been entered.
	 * @param comment An optional comment that will be logged
	 */
	@CompilerStripCall(LogLevel.METHOD)
	public void enterMethod(String comment) {
		delegate.methodImpl(true, comment, 2);
	}
	
	/**
	 * Logs a message at {@link LogLevel#METHOD} that the calling method has been exited.
	 * @param comment An optional comment that will be logged
	 */
	@CompilerStripCall(LogLevel.METHOD)
	public void exitMethod(String comment) {
		delegate.methodImpl(false, comment, 2);
	}
	
	/**
	 * Logs a message with {@link LogLevel#DEBUG}.
	 * @param message The message to log
	 * @see #log(AbstractLogLevel, String)
	 */
	@CompilerStripCall(LogLevel.DEBUG)
	public void debug(String message) {
		log(LogLevel.DEBUG, message);
	}
	
	/**
	 * Logs a message with {@link LogLevel#DEBUG}.
	 * The supplier will not be called if the log level is below {@link #getLogLevel()}.
	 * @param message A {@link Supplier} that generates the message
	 * @see #log(AbstractLogLevel, Supplier)
	 */
	@CompilerStripCall(LogLevel.DEBUG)
	public void debug(Supplier<String> message) {
		log(LogLevel.DEBUG, message);
	}
	
	/**
	 * Logs a formatted message with {@link LogLevel#DEBUG}.
	 * The format string and the objects will be combined into a single string using {@link String#format(String, Object...)}.
	 * The string will not be formatted if the log level is below {@link #getLogLevel()}.
	 * @param formatString The format string as defined in {@link Formatter}.
	 * @param objects The objects to process with the format string.
	 * @see #log(AbstractLogLevel, String, Object...)
	 */
	@CompilerStripCall(LogLevel.DEBUG)
	public void debug(String formatString, Object...objects) {
		log(LogLevel.DEBUG, formatString, objects);
	}
	
	/**
	 * Logs a message with {@link LogLevel#INFO}.
	 * @param message The message to log
	 * @see #log(AbstractLogLevel, String)
	 */
	@CompilerStripCall(LogLevel.INFO)
	public void info(String message) {
		log(LogLevel.INFO, message);
	}
	
	/**
	 * Logs a message with {@link LogLevel#INFO}.
	 * The supplier will not be called if the log level is below {@link #getLogLevel()}.
	 * @param message A {@link Supplier} that generates the message
	 * @see #log(AbstractLogLevel, Supplier)
	 */
	@CompilerStripCall(LogLevel.INFO)
	public void info(Supplier<String> message) {
		log(LogLevel.INFO, message);
	}
	
	/**
	 * Logs a formatted message with {@link LogLevel#INFO}.
	 * The format string and the objects will be combined into a single string using {@link String#format(String, Object...)}.
	 * The string will not be formatted if the log level is below {@link #getLogLevel()}.
	 * @param formatString The format string as defined in {@link Formatter}.
	 * @param objects The objects to process with the format string.
	 * @see #log(AbstractLogLevel, String, Object...)
	 */
	@CompilerStripCall(LogLevel.INFO)
	public void info(String formatString, Object...objects) {
		log(LogLevel.INFO, formatString, objects);
	}
	
	/**
	 * Logs a message with {@link LogLevel#WARNING}.
	 * @param message The message to log
	 * @see #log(AbstractLogLevel, String)
	 */
	@CompilerStripCall(LogLevel.WARNING)
	public void warning(String message) {
		log(LogLevel.WARNING, message);
	}
	
	/**
	 * Logs a message with {@link LogLevel#WARNING}.
	 * The supplier will not be called if the log level is below {@link #getLogLevel()}.
	 * @param message A {@link Supplier} that generates the message
	 * @see #log(AbstractLogLevel, Supplier)
	 */
	@CompilerStripCall(LogLevel.WARNING)
	public void warning(Supplier<String> message) {
		log(LogLevel.WARNING, message);
	}
	
	/**
	 * Logs a formatted message with {@link LogLevel#WARNING}.
	 * The format string and the objects will be combined into a single string using {@link String#format(String, Object...)}.
	 * The string will not be formatted if the log level is below {@link #getLogLevel()}.
	 * @param formatString The format string as defined in {@link Formatter}.
	 * @param objects The objects to process with the format string.
	 * @see #log(AbstractLogLevel, String, Object...)
	 */
	@CompilerStripCall(LogLevel.WARNING)
	public void warning(String formatString, Object...objects) {
		log(LogLevel.WARNING, formatString, objects);
	}
	
	/**
	 * Logs an exception with {@link LogLevel#WARNING}.
	 * @param messageAndStacktrace The {@link Exception} to log, including the exception stacktrace
	 * @see #log(AbstractLogLevel, Exception)
	 */
	@CompilerStripCall(LogLevel.WARNING)
	public void warning(Exception messageAndStacktrace) {
		log(LogLevel.WARNING, messageAndStacktrace);
	}
	
	/**
	 * Logs a message and exception with {@link LogLevel#WARNING}.
	 * @param message A message with additional detail to log before the exception
	 * @param stacktrace The {@link Exception} to log, including the exception stacktrace
	 * @see #log(AbstractLogLevel, String, Exception)
	 */
	@CompilerStripCall(LogLevel.WARNING)
	public void warning(String message, Exception stacktrace) {
		log(LogLevel.WARNING, message, stacktrace);
	}
	
	/**
	 * Logs a message with {@link LogLevel#ERROR}.
	 * @param message The message to log
	 * @see #log(AbstractLogLevel, String)
	 */
	@CompilerStripCall(LogLevel.ERROR)
	public void error(String message) {
		log(LogLevel.ERROR, message);
	}
	
	/**
	 * Logs a message with {@link LogLevel#ERROR}.
	 * The supplier will not be called if the log level is below {@link #getLogLevel()}.
	 * @param message A {@link Supplier} that generates the message
	 * @see #log(AbstractLogLevel, Supplier)
	 */
	@CompilerStripCall(LogLevel.ERROR)
	public void error(Supplier<String> message) {
		log(LogLevel.ERROR, message);
	}
	
	/**
	 * Logs a formatted message with {@link LogLevel#ERROR}.
	 * The format string and the objects will be combined into a single string using {@link String#format(String, Object...)}.
	 * The string will not be formatted if the log level is below {@link #getLogLevel()}.
	 * @param formatString The format string as defined in {@link Formatter}.
	 * @param objects The objects to process with the format string.
	 * @see #log(AbstractLogLevel, String, Object...)
	 */
	@CompilerStripCall(LogLevel.ERROR)
	public void error(String formatString, Object...objects) {
		log(LogLevel.ERROR, formatString, objects);
	}
	
	/**
	 * Logs an exception with {@link LogLevel#ERROR}.
	 * @param messageAndStacktrace The {@link Exception} to log, including the exception stacktrace
	 * @see #log(AbstractLogLevel, Exception)
	 */
	@CompilerStripCall(LogLevel.ERROR)
	public void error(Exception messageAndStacktrace) {
		log(LogLevel.ERROR, messageAndStacktrace);
	}
	
	/**
	 * Logs a message and exception with {@link LogLevel#ERROR}.
	 * @param message A message with additional detail to log before the exception
	 * @param stacktrace The {@link Exception} to log, including the exception stacktrace
	 * @see #log(AbstractLogLevel, String, Exception)
	 */
	@CompilerStripCall(LogLevel.ERROR)
	public void error(String message, Exception stacktrace) {
		log(LogLevel.ERROR, message, stacktrace);
	}
	
	/**
	 * Logs a message with {@link LogLevel#FATAL}.
	 * @param message The message to log
	 * @see #log(AbstractLogLevel, String)
	 */
	@CompilerStripCall(LogLevel.FATAL)
	public void fatal(String message) {
		log(LogLevel.FATAL, message);
	}
	
	/**
	 * Logs a message with {@link LogLevel#FATAL}.
	 * The supplier will not be called if the log level is below {@link #getLogLevel()}.
	 * @param message A {@link Supplier} that generates the message
	 * @see #log(AbstractLogLevel, Supplier)
	 */
	@CompilerStripCall(LogLevel.FATAL)
	public void fatal(Supplier<String> message) {
		log(LogLevel.FATAL, message);
	}
	
	/**
	 * Logs a formatted message with {@link LogLevel#FATAL}.
	 * The format string and the objects will be combined into a single string using {@link String#format(String, Object...)}.
	 * The string will not be formatted if the log level is below {@link #getLogLevel()}.
	 * @param formatString The format string as defined in {@link Formatter}.
	 * @param objects The objects to process with the format string.
	 * @see #log(AbstractLogLevel, String, Object...)
	 */
	@CompilerStripCall(LogLevel.FATAL)
	public void fatal(String formatString, Object...objects) {
		log(LogLevel.FATAL, formatString, objects);
	}
	
	/**
	 * Logs an exception with {@link LogLevel#FATAL}.
	 * @param messageAndStacktrace The {@link Exception} to log, including the exception stacktrace
	 * @see #log(AbstractLogLevel, Exception)
	 */
	@CompilerStripCall(LogLevel.FATAL)
	public void fatal(Exception messageAndStacktrace) {
		log(LogLevel.FATAL, messageAndStacktrace);
	}
	
	/**
	 * Logs a message and exception with {@link LogLevel#FATAL}.
	 * @param message A message with additional detail to log before the exception
	 * @param stacktrace The {@link Exception} to log, including the exception stacktrace
	 * @see #log(AbstractLogLevel, String, Exception)
	 */
	@CompilerStripCall(LogLevel.FATAL)
	public void fatal(String message, Exception stacktrace) {
		log(LogLevel.FATAL, message, stacktrace);
	}
	
	/**
	 * Logs a message with the given log level.
	 * If the log level is below {@link #getLogLevel()}, the delegate will not receive the message.
	 * @param level The {@link AbstractLogLevel} for the message
	 * @param message The message to log
	 */
	public void log(AbstractLogLevel level, String message) {
		if(canLog(level)) {
			delegate.logImpl(level, message, null);
		}
	}
	
	/**
	 * Logs a message with the given log level.
	 * If the log level is below {@link #getLogLevel()}, the delegate will not receive the message
	 * and the supplier will not be called.
	 * @param level The {@link AbstractLogLevel} for the message
	 * @param message The message to log
	 */
	public void log(AbstractLogLevel level, Supplier<String> message) {
		if(canLog(level)) {
			delegate.logImpl(level, message.get(), null);
		}
	}
	
	/**
	 * Logs a formatted message with the given log level.
	 * The format string and the objects will be combined into a single string using {@link String#format(String, Object...)}.
	 * If the log level is below {@link #getLogLevel()}, the delegate will not receive the
	 * message and the message will never be formatted.
	 * @param level The {@link AbstractLogLevel} for the message
	 * @param formatString The format string as defined in {@link Formatter}.
	 * @param objects The objects to process with the format string.
	 */
	public void log(AbstractLogLevel level, String formatString, Object...objects) {
		if(canLog(level)) {
			delegate.logImpl(level, String.format(formatString, objects), null);
		}
	}

	/**
	 * Logs an exception with the given log level.
	 * If the log level is below {@link #getLogLevel()}, the delegate will not receive the message.
	 * @param level The {@link AbstractLogLevel} for the message
	 * @param messageAndStacktrace The {@link Exception} to log, including the exception stacktrace
	 */
	public void log(AbstractLogLevel level, Exception messageAndStacktrace) {
		if(canLog(level)) {
			delegate.logImpl(level, null, messageAndStacktrace);
		}
	}
	
	/**
	 * Logs a message and exception with the given log level.
	 * If the log level is below {@link #getLogLevel()}, the delegate will not receive the message.
	 * @param level The {@link AbstractLogLevel} for the message
	 * @param message A message with additional detail to log before the exception
	 * @param stacktrace The {@link Exception} to log, including the exception stacktrace
	 */
	public void log(AbstractLogLevel level, String message, Exception stacktrace) {
		if(canLog(level)) {
			delegate.logImpl(level, message, stacktrace);
		}
	}
	
	/**
	 * Prints the current stack trace at {@link LogLevel#METHOD}. The call to this method will always be the top displayed stack frame.
	 */
	@CompilerStripCall(LogLevel.METHOD)
	public void stack() {
		delegate.stackImpl(LogLevel.METHOD, 1, null);
	}
	
	/**
	 * Prints the current stack trace at {@link LogLevel#METHOD}. The call to this method will always be the top displayable stack frame.
	 * The actual top stack frame depends on the {@code popEntries} parameter.
	 * @param popEntries Amount of stack frames to pop off the top of the displayed stack. Must not be negative
	 */
	@CompilerStripCall(LogLevel.METHOD)
	public void stack(int popEntries) {
		delegate.stackImpl(LogLevel.METHOD, popEntries + 1, null);
	}
	
	/**
	 * Prints the current stack trace at {@link LogLevel#METHOD}. The call to this method will always be the top displayed stack frame.
	 * @param comment An additional comment that will be printed with the stack trace
	 */
	@CompilerStripCall(LogLevel.METHOD)
	public void stack(String comment) {
		delegate.stackImpl(LogLevel.METHOD, 1, comment);
	}
	
	/**
	 * Prints the current stack trace at {@link LogLevel#METHOD}. The call to this method will always be the top displayable stack frame.
	 * The actual top stack frame depends on the {@code popEntries} parameter.
	 * @param popEntries Amount of stack frames to pop off the top of the displayed stack. Must not be negative
	 * @param comment An additional comment that will be printed with the stack trace
	 */
	@CompilerStripCall(LogLevel.METHOD)
	public void stack(int popEntries, String comment) {
		delegate.stackImpl(LogLevel.METHOD, popEntries, comment);
	}
	
	/**
	 * Prints the current stack trace. The call to this method will always be the top displayed stack frame.
	 * @param level The {@link AbstractLogLevel} to log the stacktrace at
	 */
	public void stack(AbstractLogLevel level) {
		delegate.stackImpl(level, 1, null);
	}
	
	/**
	 * Prints the current stack trace. The call to this method will always be the top displayable stack frame.
	 * The actual top stack frame depends on the {@code popEntries} parameter.
	 * @param level The {@link AbstractLogLevel} to log the stacktrace at
	 * @param popEntries Amount of stack frames to pop off the top of the displayed stack. Must not be negative
	 */
	public void stack(AbstractLogLevel level, int popEntries) {
		delegate.stackImpl(level, popEntries + 1, null);
	}
	
	/**
	 * Prints the current stack trace. The call to this method will always be the top displayed stack frame.
	 * @param level The {@link AbstractLogLevel} to log the stacktrace at
	 * @param comment An additional comment that will be printed with the stack trace
	 */
	public void stack(AbstractLogLevel level, String comment) {
		delegate.stackImpl(level, 1, comment);
	}
	
	/**
	 * Prints the current stack trace. The call to this method will always be the top displayable stack frame.
	 * The actual top stack frame depends on the {@code popEntries} parameter.
	 * @param level The {@link AbstractLogLevel} to log the stacktrace at
	 * @param popEntries Amount of stack frames to pop off the top of the displayed stack. Must not be negative
	 * @param comment An additional comment that will be printed with the stack trace
	 */
	public void stack(AbstractLogLevel level, int popEntries, String comment) {
		delegate.stackImpl(level, popEntries, comment);
	}
	
	/**
	 * The current lowest log level. Any call of a log method with a lower level will not be
	 * passed to the delegate.
	 * @return The current lowest log level
	 */
	public AbstractLogLevel getLogLevel() {
		return cutoffLevel.get();
	}
	
	/**
	 * Sets the current lowest log level. Any call of a log method with a lower level will not be
	 * passed to the delegate.
	 * @param logLevel The new lowest log level
	 */
	public void setLogLevel(AbstractLogLevel logLevel) {
		cutoffLevel.set(Objects.requireNonNull(logLevel, "'logLevel' parameter must not be null"));
	}
}
