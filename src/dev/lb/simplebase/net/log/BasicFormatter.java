package dev.lb.simplebase.net.log;

import dev.lb.simplebase.net.annotation.Internal;

@Internal
abstract class BasicFormatter implements Formatter {

	protected abstract CharSequence format(AbstractLogLevel level);
	
	@Override
	public CharSequence formatPlaintext(AbstractLogLevel level, String rawMessage) {
		return format(level);
	}

	@Override
	public CharSequence formatException(AbstractLogLevel level, String message, Exception rawMessage) {
		return format(level);
	}

	@Override
	public CharSequence formatStacktrace(AbstractLogLevel level, String comment, StackTraceElement[] usedStacktraceItems) {
		return format(level);
	}

	@Override
	public CharSequence formatMethod(AbstractLogLevel level, String comment, boolean enteringMethod, StackTraceElement callingMethodElement) {
		return format(level);
	}

}
