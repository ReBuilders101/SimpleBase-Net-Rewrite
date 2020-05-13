package dev.lb.simplebase.net.log;

import java.util.function.Supplier;

import dev.lb.simplebase.net.annotation.Internal;

/**
 * A logger that does nothing at all
 */
@Internal
class EmptyLogger implements AbstractLogger {

	private static final AbstractLogLevel LOWEST_POSSIBLE = CustomLogLevel.create(Integer.MIN_VALUE);
	
	@Override
	public void enterMethod(String comment) {}
	@Override
	public void exitMethod(String comment) {}

	@Override
	public void log(AbstractLogLevel level, String message) {}
	@Override
	public void log(AbstractLogLevel level, Supplier<String> message) {}
	@Override
	public void log(AbstractLogLevel level, String formatString, Object... objects) {}
	@Override
	public void log(AbstractLogLevel level, Exception messageAndStacktrace) {}

	@Override
	public void stack(AbstractLogLevel level) {}
	@Override
	public void stack(AbstractLogLevel level, int popEntries) {}
	@Override
	public void stack(AbstractLogLevel level, String comment) {}
	@Override
	public void stack(AbstractLogLevel level, int popEntries, String comment) {}

	@Override
	public AbstractLogLevel getLogLevel() {
		return LOWEST_POSSIBLE;
	}

	@Override
	public void setLogLevel(AbstractLogLevel logLevel) {}

}
