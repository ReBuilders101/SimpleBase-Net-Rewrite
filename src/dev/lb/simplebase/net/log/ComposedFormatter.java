package dev.lb.simplebase.net.log;

import java.text.DateFormat;
import java.util.function.Function;

import dev.lb.simplebase.net.annotation.Internal;

/**
 * Instantiated by the {@link Formatter#compose(Formatter, Function)} method
 */
@Internal
class ComposedFormatter implements Formatter{

	//This is just here because you cant put private members in interfaces
	protected static final DateFormat DATE_TO_TIME = DateFormat.getTimeInstance();
	
	private final Formatter base;
	private final Function<String, CharSequence> patch;
	
	protected ComposedFormatter(Formatter base, Function<String, CharSequence> patch) {
		this.base = base;
		this.patch = patch;
	}
	
	@Override
	public CharSequence formatPlaintext(AbstractLogLevel level, String rawMessage) {
		return patch.apply(base.formatPlaintext(level, rawMessage).toString());
	}

	@Override
	public CharSequence formatException(AbstractLogLevel level, String message, Exception rawMessage) {
		return patch.apply(base.formatException(level, message, rawMessage).toString());
	}

	@Override
	public CharSequence formatStacktrace(AbstractLogLevel level, String comment, StackTraceElement[] usedStacktraceItems) {
		return patch.apply(base.formatStacktrace(level, comment, usedStacktraceItems).toString());
	}

	@Override
	public CharSequence formatMethod(AbstractLogLevel level, String comment, boolean enteringMethod, StackTraceElement callingMethodElement) {
		return patch.apply(base.formatMethod(level, comment, enteringMethod, callingMethodElement).toString());
	}

	@Override
	public String toString() {
		return "ComposedFormatter [base=" + base + ", patch=" + patch + "]";
	}

}
