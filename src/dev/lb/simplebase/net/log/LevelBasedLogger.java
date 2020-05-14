package dev.lb.simplebase.net.log;

import java.util.Objects;
import java.util.function.Supplier;
/**
 * An abstract base class for loggers that store a lowest {@link AbstractLogLevel}.
 * Will automatically discard any log calls below the required level.
 * <p>
 * Create {@link AbstractLogger} instances using the {@link Loggers} class.
 * This can be used as a base class for a custom {@code AbstractLogger} implementation.
 */
public abstract class LevelBasedLogger implements AbstractLogger {

	protected LevelBasedLogger(AbstractLogLevel cutoff, boolean supportsLevelChange) {
		Objects.requireNonNull(cutoff, "'cutoff' parameter must not be null");
		this.cutoff = cutoff;
		this.supportsLevelChange = supportsLevelChange;
	}
	
	private AbstractLogLevel cutoff;
	private boolean supportsLevelChange;

	/**
	 * Called when a log call to {@link #enterMethod()} / {@link #exitMethod()} was performed
	 * with the correct logging level
	 * @param level The log level of the message
	 * @param comment An optional message to log with the enter/exit notice
	 * @param enter {@code true} if method was entered, {@code false} if it was exited
	 * @param stackPop The amount of entries to pop off the top of the stack to find the one for the method that was entered/exited
	 */
	protected abstract void methodImpl(AbstractLogLevel level, String comment, boolean enter, int stackPop);
	
	/**
	 * Called when a log call with the correct logging level was performed
	 * @param level The log level of the message
	 * @param message The message that should be logged
	 */
	protected abstract void logImpl(AbstractLogLevel level, String message);
	
	/**
	 * Called when a log call with the correct logging level was performed
	 * @param level The log level of the message
	 * @param messageAndStacktrace The exception that should be logged
	 */
	protected abstract void logImpl(AbstractLogLevel level, Exception messageAndStacktrace);
	
	/**
	 * Called when a call to {@link #stack(AbstractLogLevel)} (or any other overload)
	 * was performed with the correct logging level
	 * @param level The log level of the message
	 * @param comment An optional message to log with the stacktrace notice
	 * @param stackPop The amount of entries to pop off the top of the stack
	 */
	protected abstract void stackImpl(AbstractLogLevel level, String comment, int stackPop);
	
	@Override
	public void enterMethod(String comment) {
		if(LogLevel.METHOD.isAbove(cutoff)) methodImpl(LogLevel.METHOD, comment, true, 3); //this and impl
	}

	@Override
	public void exitMethod(String comment) {
		if(LogLevel.METHOD.isAbove(cutoff)) methodImpl(LogLevel.METHOD, comment, false, 3); //this and impl
	}

	@Override
	public void log(AbstractLogLevel level, String message) {
		Objects.requireNonNull(level, "'level' parameter must not be null");
		if(level.isAbove(cutoff)) logImpl(level, message);
	}

	@Override
	public void log(AbstractLogLevel level, Supplier<String> message) {
		Objects.requireNonNull(level, "'level' parameter must not be null");
		if(level.isAbove(cutoff)) logImpl(level, message.get()); //Only get when required
	}

	@Override
	public void log(AbstractLogLevel level, String formatString, Object... objects) {
		Objects.requireNonNull(level, "'level' parameter must not be null");
		if(level.isAbove(cutoff)) logImpl(level, String.format(formatString, objects)); //Only format when required
	}

	@Override
	public void log(AbstractLogLevel level, Exception messageAndStacktrace) {
		Objects.requireNonNull(level, "'level' parameter must not be null");
		if(level.isAbove(cutoff)) logImpl(level, messageAndStacktrace);
	}

	@Override
	public void stack(AbstractLogLevel level) {
		Objects.requireNonNull(level, "'level' parameter must not be null");
		if(level.isAbove(cutoff)) stackImpl(level, null, 3); //pop this method and stackImpl
	}

	@Override
	public void stack(AbstractLogLevel level, int popEntries) {
		Objects.requireNonNull(level, "'level' parameter must not be null");
		if(level.isAbove(cutoff)) stackImpl(level, null, popEntries + 3); //pop this method and stackImpl
	}

	@Override
	public void stack(AbstractLogLevel level, String comment) {
		Objects.requireNonNull(level, "'level' parameter must not be null");
		if(level.isAbove(cutoff)) stackImpl(level, comment, 3); //pop this method and stackImpl
	}

	@Override
	public void stack(AbstractLogLevel level, int popEntries, String comment) {
		Objects.requireNonNull(level, "'level' parameter must not be null");
		if(level.isAbove(cutoff)) stackImpl(level, comment, popEntries + 3); //pop this method and stackImpl
	}

	@Override
	public AbstractLogLevel getLogLevel() {
		return cutoff;
	}

	@Override
	public void setLogLevel(AbstractLogLevel logLevel) {
		Objects.requireNonNull(logLevel, "'logLevel' parameter must not be null");
		if(!supportsLevelChange) throw new UnsupportedOperationException("This AbstractLogger implementation does not allow changing the Log Level");
		cutoff = logLevel;
	}
	

}
