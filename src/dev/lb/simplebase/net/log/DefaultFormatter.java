package dev.lb.simplebase.net.log;

import dev.lb.simplebase.net.annotation.Internal;

@Internal
class DefaultFormatter implements Formatter {

	private static final String NULL_STR = "null"; //could be an empty string as well, maybe?

	@Override
	public CharSequence formatPlaintext(AbstractLogLevel level, String rawMessage) {
		return rawMessage == null ? NULL_STR : rawMessage;
	}

	@Override
	public CharSequence formatException(AbstractLogLevel level, String message, Exception rawMessage) {
		if(rawMessage == null) {
			return NULL_STR;
		} else {
			final StringBuilder builder = new StringBuilder();
			if(message != null)	builder.append("\n").append(message);
			builder.append("\nException: ");
			builder.append(rawMessage.getClass().getCanonicalName());
			builder.append("\nMessage: ");
			builder.append(rawMessage.getMessage());
			builder.append("\nStacktrace: ");
			appendStacktrace(builder, rawMessage.getStackTrace());
			return builder;
		}
	}

	/**
	 * Uses a <b>leading</b> line break between stack trace elements
	 * @param builder
	 * @param elements
	 */
	protected static void appendStacktrace(StringBuilder builder, StackTraceElement[] elements) {
		for(StackTraceElement e : elements) {
			builder.append("\n	"); //indent with one tab
			builder.append(e);
		}
	}

	@Override
	public CharSequence formatStacktrace(AbstractLogLevel level, String comment, StackTraceElement[] usedStacktraceItems) {
		final StringBuilder builder = new StringBuilder();
		if(comment != null) {
			builder.append("Message: ");
			builder.append(comment);
			builder.append('\n');
		}
		builder.append("Stacktrace: ");
		appendStacktrace(builder, usedStacktraceItems);
		return builder;
	}

	@Override
	public CharSequence formatMethod(AbstractLogLevel level, String comment, boolean enteringMethod, StackTraceElement callingMethodElement) {
		final StringBuilder builder = new StringBuilder();
		if(comment != null) {
			builder.append("Message: ");
			builder.append(comment);
			builder.append('\n');
		}
		builder.append(enteringMethod ? "Entering Method: " : "Exiting Method: ");
		builder.append(callingMethodElement);
		return builder;
	}

}
