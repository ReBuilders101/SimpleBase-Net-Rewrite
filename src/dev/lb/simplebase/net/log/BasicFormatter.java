package dev.lb.simplebase.net.log;

public interface BasicFormatter extends Formatter {

	public CharSequence format(AbstractLogLevel level);
	
	@Override
	public default CharSequence formatPlaintext(AbstractLogLevel level, String rawMessage) {
		return format(level);
	}

	@Override
	public default CharSequence formatException(AbstractLogLevel level, String message, Exception rawMessage) {
		return format(level);
	}

	@Override
	public default CharSequence formatStacktrace(AbstractLogLevel level, String comment, StackTraceElement[] usedStacktraceItems) {
		return format(level);
	}

	@Override
	public default CharSequence formatMethod(AbstractLogLevel level, String comment, boolean enteringMethod, StackTraceElement callingMethodElement) {
		return format(level);
	}

}
