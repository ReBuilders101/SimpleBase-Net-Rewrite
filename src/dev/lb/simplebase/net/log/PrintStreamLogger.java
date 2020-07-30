package dev.lb.simplebase.net.log;

import java.io.PrintStream;
import java.util.Objects;

/**
 * An {@link AbstractLogger} implementation that prints all logged text to a {@link PrintStream}.<br>
 * Can be used to log to {@link System#out}.
 * <p>
 * Create {@link AbstractLogger} instances using the {@link Loggers} class.
 */
public class PrintStreamLogger extends FormattableLogger {

	private final PrintStream stream;
	
	protected PrintStreamLogger(AbstractLogLevel cutoff, boolean supportsLevelChange, BasicFormatter prefix, Formatter formatter, PrintStream stream) {
		super(cutoff, supportsLevelChange, prefix, formatter);
		Objects.requireNonNull(stream, "'stream' parameter must not be null");
		this.stream = stream;
	}

	@Override
	protected void postLogMessage(CharSequence prefix, CharSequence message) {
		stream.println(prefix.toString() + " " + message.toString());
	}

}
