package dev.lb.simplebase.net.log;

import java.util.Arrays;
import java.util.Collection;

import dev.lb.simplebase.net.annotation.Internal;

@Internal
class ComplexFormatter implements Formatter {

	private final Formatter[] parts;

	protected ComplexFormatter(Formatter[] parts) {
		this.parts = Arrays.copyOf(parts, parts.length);
	}
	
	protected ComplexFormatter(Collection<Formatter> parts) {
		this.parts = new Formatter[parts.size()];
		parts.toArray(this.parts);
	}
	
	@Override
	public CharSequence formatPlaintext(AbstractLogLevel level, String rawMessage) {
		CharSequence[] partStrings = new CharSequence[parts.length];
		for(int i = 0; i < parts.length; i++) {
			partStrings[i] = parts[i].formatPlaintext(level, rawMessage);
		}
		return String.join(" ", partStrings);
	}

	@Override
	public CharSequence formatException(AbstractLogLevel level, String message, Exception rawMessage) {
		CharSequence[] partStrings = new CharSequence[parts.length];
		for(int i = 0; i < parts.length; i++) {
			partStrings[i] = parts[i].formatException(level, message, rawMessage);
		}
		return String.join(" ", partStrings);
	}

	@Override
	public CharSequence formatStacktrace(AbstractLogLevel level, String comment, StackTraceElement[] usedStacktraceItems) {
		CharSequence[] partStrings = new CharSequence[parts.length];
		for(int i = 0; i < parts.length; i++) {
			partStrings[i] = parts[i].formatStacktrace(level, comment, usedStacktraceItems);
		}
		return String.join(" ", partStrings);
	}

	@Override
	public CharSequence formatMethod(AbstractLogLevel level, String comment, boolean enteringMethod, StackTraceElement callingMethodElement) {
		CharSequence[] partStrings = new CharSequence[parts.length];
		for(int i = 0; i < parts.length; i++) {
			partStrings[i] = parts[i].formatMethod(level, comment, enteringMethod, callingMethodElement);
		}
		return String.join(" ", partStrings);
	}
	
}
