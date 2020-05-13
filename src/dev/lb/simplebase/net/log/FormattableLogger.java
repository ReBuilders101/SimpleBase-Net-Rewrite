package dev.lb.simplebase.net.log;

import java.util.Objects;

/**
 * An abstract base class for loggers that use a {@link Formatter} to generate log outputs.
 * Includes all features of a {@link LevelBasedLogger}.
 * <p>
 * Create {@link AbstractLogger} instances using the {@link Loggers} class.
 * This can be used as a base class for a custom {@code AbstractLogger} implementation.
 */
public abstract class FormattableLogger extends LevelBasedLogger {
	
	private final Formatter formatter;
	
	protected FormattableLogger(AbstractLogLevel cutoff, boolean supportsLevelChange, Formatter formatter) {
		super(cutoff, supportsLevelChange);
		Objects.requireNonNull(formatter, "'formatter' parameter must not be null");
		this.formatter = formatter;
	}

	/**
	 * Post a finished log message
	 * @param message The message
	 */
	protected abstract void postLogMessage(CharSequence message);

	@Override
	protected void methodImpl(AbstractLogLevel level, String comment, boolean enter, int stackPop) {
		//get the entire stack
		final StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();
		//We have to access currentStack[stackPop]
		if(stackPop > currentStack.length - 1) {
			throw new IllegalArgumentException("Cannot get stack element " + stackPop + " for logging");
		}
		final StackTraceElement callingMethodElement = currentStack[stackPop];
		postLogMessage(formatter.formatMethod(level, comment, enter, callingMethodElement));
	}

	@Override
	protected void logImpl(AbstractLogLevel level, String message) {
		postLogMessage(formatter.formatPlaintext(level, message));
	}

	@Override
	protected void logImpl(AbstractLogLevel level, Exception messageAndStacktrace) {
		postLogMessage(formatter.formatException(level, messageAndStacktrace));
	}

	@Override
	protected void stackImpl(AbstractLogLevel level, String comment, int stackPop) {
		final StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();
		if(stackPop > currentStack.length - 1) {
			throw new IllegalArgumentException("Cannot get stack element " + stackPop + " for logging");
		}
		final StackTraceElement[] partialStack = new StackTraceElement[currentStack.length - stackPop];
		System.arraycopy(currentStack, stackPop, partialStack, 0, currentStack.length - stackPop);
		postLogMessage(formatter.formatStacktrace(level, comment, partialStack));
	}
	
}
