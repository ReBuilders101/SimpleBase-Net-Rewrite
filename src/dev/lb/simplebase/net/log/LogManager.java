package dev.lb.simplebase.net.log;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import dev.lb.simplebase.net.annotation.StaticType;
import dev.lb.simplebase.net.log.DefaultLoggerDelegates.FormattingLoggerDelegate;
import dev.lb.simplebase.net.log.Logger.LoggerDelegate;

/**
 * The {@link LogManager} class provides static methods to create {@link Logger} instances.
 */
@StaticType
public final class LogManager {
	private LogManager() {}
	
	/**
	 * A {@link Logger} that prints all messages to the {@link System#out} stream.
	 * @param minLogLevel The initial minimum log level to log messages at
	 * @return A {@link Logger} for {@link System#out}
	 */
	public static Logger standardOut(AbstractLogLevel minLogLevel) {
		return printStream(minLogLevel, System.out);
	}
	
	/**
	 * A {@link Logger} that prints all messages to the {@link System#err} stream.
	 * @param minLogLevel The initial minimum log level to log messages at
	 * @return A {@link Logger} for {@link System#err}
	 */
	public static Logger standardErr(AbstractLogLevel minLogLevel) {
		return printStream(minLogLevel, System.err);
	}
	
	/**
	 * A {@link Logger} that prints all messages to a {@link PrintStream}.
	 * @param minLogLevel The initial minimum log level to log messages at
	 * @param outputStream The {@link PrintStream} to print to
	 * @return A {@link Logger} for the stream
	 */
	public static Logger printStream(AbstractLogLevel minLogLevel, PrintStream outputStream) {
		Objects.requireNonNull(minLogLevel, "'minLogLevel' parameter must not be null");
		Objects.requireNonNull(outputStream, "'outputStream' parameter must not be null");
		
		return new Logger(DefaultLoggerDelegates.forSingleStream(outputStream), new AtomicReference<>(minLogLevel));
	}
	
	/**
	 * A {@link Logger} that prints to {@link System#out} or {@link System#err} depending on the log
	 * level of the message.
	 * @param minLogLevel The initial minimum log level to log messages at
	 * @param stderrLevel The minimum required log level to print on {@code err} instead of {@code out}
	 * @return A {@link Logger} that dynamically decides the stream to use
	 */
	public static Logger standardDynamic(AbstractLogLevel minLogLevel, AbstractLogLevel stderrLevel) {
		Objects.requireNonNull(minLogLevel, "'minLogLevel' parameter must not be null");
		Objects.requireNonNull(stderrLevel, "'stderrLevel' parameter must not be null");
		
		return new Logger(DefaultLoggerDelegates.forDualStream(System.out, System.err, stderrLevel), new AtomicReference<>(minLogLevel));
	}
	
	/**
	 * A formatted {@link Logger} that prints all messages to the {@link System#out} stream.
	 * @param minLogLevel The initial minimum log level to log messages at
	 * @param formats The {@link SimplePrefixFormat}s to apply to the message
	 * @return A {@link Logger} for {@link System#out}
	 */
	public static Logger standardOut(AbstractLogLevel minLogLevel, SimplePrefixFormat...formats) {
		return printStream(minLogLevel, System.out, formats);
	}
	
	/**
	 * A formatted {@link Logger} that prints all messages to the {@link System#err} stream.
	 * @param minLogLevel The initial minimum log level to log messages at
	 * @param formats The {@link SimplePrefixFormat}s to apply to the message
	 * @return A {@link Logger} for {@link System#err}
	 */
	public static Logger standardErr(AbstractLogLevel minLogLevel, SimplePrefixFormat...formats) {
		return printStream(minLogLevel, System.err, formats);
	}
	
	/**
	 * A formatted {@link Logger} that prints all messages to a {@link PrintStream}.
	 * @param minLogLevel The initial minimum log level to log messages at
	 * @param outputStream The {@link PrintStream} to print to
	 * @param formats The {@link SimplePrefixFormat}s to apply to the message
	 * @return A {@link Logger} for the stream
	 */
	public static Logger printStream(AbstractLogLevel minLogLevel, PrintStream outputStream, SimplePrefixFormat...formats) {
		Objects.requireNonNull(minLogLevel, "'minLogLevel' parameter must not be null");
		Objects.requireNonNull(outputStream, "'outputStream' parameter must not be null");
		
		return new Logger(DefaultLoggerDelegates.forFormattedSingleStream(outputStream, formats), new AtomicReference<>(minLogLevel));
	}
	
	/**
	 * A {@link Logger} that prints to {@link System#out} or {@link System#err} depending on the log
	 * level of the message.
	 * @param minLogLevel The initial minimum log level to log messages at
	 * @param stderrLevel The minimum required log level to print on {@code err} instead of {@code out}
	 * @param formats The {@link SimplePrefixFormat}s to apply to the message
	 * @return A {@link Logger} that dynamically decides the stream to use
	 */
	public static Logger standardDynamic(AbstractLogLevel minLogLevel, AbstractLogLevel stderrLevel, SimplePrefixFormat...formats) {
		Objects.requireNonNull(minLogLevel, "'minLogLevel' parameter must not be null");
		Objects.requireNonNull(stderrLevel, "'stderrLevel' parameter must not be null");
		
		return new Logger(DefaultLoggerDelegates.forFormattedDualStream(System.out, System.err, stderrLevel, formats), new AtomicReference<>(minLogLevel));
	}
	
	/**
	 * Derives a {@link Logger} with a different set of {@link SimplePrefixFormat}s.
	 * @param base The base logger to derive from
	 * @param operator An {@link UnaryOperator} that determines
	 * the prefixes of the derived logger based on the prefixes of the base logger
	 * @return A new {@link Logger} with the same delegate, but different formats 
	 */
	public static Logger derive(Logger base, UnaryOperator<Collection<SimplePrefixFormat>> operator) {
		Objects.requireNonNull(base, "'base' parameter must not be null");
		Objects.requireNonNull(operator, "'operator' parameter must not be null");
		
		LoggerDelegate del = base.delegate;
		final LoggerDelegate newLD;
		if(del instanceof FormattingLoggerDelegate) {
			FormattingLoggerDelegate del2 = (FormattingLoggerDelegate) del;
			Collection<SimplePrefixFormat> newFormat = operator.apply(del2.prefixes);
			newLD = new FormattingLoggerDelegate(del2.base, newFormat);
		} else {
			Collection<SimplePrefixFormat> newFormat = operator.apply(new ArrayList<>());
			newLD = new FormattingLoggerDelegate(del, newFormat);
		}
		return new Logger(newLD, base.cutoffLevel);
	}
	
	/**
	 * Creates a {@link Logger} using a custom {@link LoggerDelegate} to log messages.
	 * @param minLogLevel The initial minimum log level to log messages at
	 * @param loggerDelegate The {@link LoggerDelegate} that handles messages that should be logged
	 * @return A {@link Logger} with a custom delegate
	 */
	public static Logger customDelegate(AbstractLogLevel minLogLevel, Logger.LoggerDelegate loggerDelegate) {
		Objects.requireNonNull(minLogLevel, "'minLogLevel' parameter must not be null");
		Objects.requireNonNull(loggerDelegate, "'loggerDelegate' parameter must not be null");
		
		return new Logger(loggerDelegate, new AtomicReference<>(minLogLevel));
	}
}
