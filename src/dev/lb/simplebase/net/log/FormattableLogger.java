package dev.lb.simplebase.net.log;

/**
 * An abstract base class for loggers that use a {@link Formatter} to generate log outputs.
 * Includes all features of a {@link LevelBasedLogger}.
 * <p>
 * Create {@link AbstractLogger} instances using the {@link Loggers} class.
 * This can be used as a base class for a custom {@code AbstractLogger} implementation.
 */
public abstract class FormattableLogger extends LevelBasedLogger {
	
	private final BasicFormatter prefixFormatter;
	private final Formatter messageFormatter;
	
	protected FormattableLogger(AbstractLogLevel cutoff, boolean supportsLevelChange, BasicFormatter prefixFormatter, Formatter messageFormatter) {
		super(cutoff, supportsLevelChange);
		this.messageFormatter = messageFormatter;
		this.prefixFormatter = prefixFormatter;
	}

	/**
	 * Post a finished log message
	 * @param message The message
	 */
	protected abstract void postLogMessage(CharSequence prefix, CharSequence message);

	protected BasicFormatter getPrefixFormatter() {
		return prefixFormatter;
	}
	
	protected Formatter getMessageFormatter() {
		return messageFormatter;
	}
	
	@Override
	protected void methodImpl(AbstractLogLevel level, String comment, boolean enter, int stackPop) {
		//get the entire stack
		final StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();
		//We have to access currentStack[stackPop]
		if(stackPop > currentStack.length - 1) {
			throw new IllegalArgumentException("Cannot get stack element " + stackPop + " for logging");
		}
		final StackTraceElement callingMethodElement = currentStack[stackPop];
		postLogMessage(getPrefixFormatter().format(level), getMessageFormatter().formatMethod(level, comment, enter, callingMethodElement));
	}

	@Override
	protected void logImpl(AbstractLogLevel level, String message) {
		postLogMessage(getPrefixFormatter().format(level), getMessageFormatter().formatPlaintext(level, message));
	}

	@Override
	protected void logImpl(AbstractLogLevel level, Exception messageAndStacktrace) {
		postLogMessage(getPrefixFormatter().format(level), getMessageFormatter().formatException(level, null, messageAndStacktrace));
	}

	@Override
	protected void logImpl(AbstractLogLevel level, String message, Exception stacktrace) {
		postLogMessage(getPrefixFormatter().format(level), getMessageFormatter().formatException(level, message, stacktrace));
	}
	
	@Override
	protected void stackImpl(AbstractLogLevel level, String comment, int stackPop) {
		final StackTraceElement[] currentStack = Thread.currentThread().getStackTrace();
		if(stackPop > currentStack.length - 1) {
			throw new IllegalArgumentException("Cannot get stack element " + stackPop + " for logging");
		}
		final StackTraceElement[] partialStack = new StackTraceElement[currentStack.length - stackPop];
		System.arraycopy(currentStack, stackPop, partialStack, 0, currentStack.length - stackPop);
		postLogMessage(getPrefixFormatter().format(level), getMessageFormatter().formatStacktrace(level, comment, partialStack));
	}
	
}
