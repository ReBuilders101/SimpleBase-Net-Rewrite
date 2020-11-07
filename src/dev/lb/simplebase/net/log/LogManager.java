package dev.lb.simplebase.net.log;

import java.io.PrintStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import dev.lb.simplebase.net.annotation.StaticType;
import dev.lb.simplebase.net.log.DefaultLoggerDelegates.FormattingLoggerDelegate;
import dev.lb.simplebase.net.log.Logger.LoggerDelegate;

@StaticType
public final class LogManager {
	private LogManager() {}
	
	public static Logger standardOut(AbstractLogLevel minLogLevel) {
		return printStream(minLogLevel, System.out);
	}
	
	public static Logger standardErr(AbstractLogLevel minLogLevel) {
		return printStream(minLogLevel, System.err);
	}
	
	public static Logger printStream(AbstractLogLevel minLogLevel, PrintStream outputStream) {
		Objects.requireNonNull(minLogLevel, "'minLogLevel' parameter must not be null");
		Objects.requireNonNull(outputStream, "'outputStream' parameter must not be null");
		
		return new Logger(DefaultLoggerDelegates.forSingleStream(outputStream), new AtomicReference<>(minLogLevel));
	}
	
	public static Logger standardDynamic(AbstractLogLevel minLogLevel, AbstractLogLevel stderrLevel) {
		Objects.requireNonNull(minLogLevel, "'minLogLevel' parameter must not be null");
		Objects.requireNonNull(stderrLevel, "'stderrLevel' parameter must not be null");
		
		return new Logger(DefaultLoggerDelegates.forDualStream(System.out, System.err, stderrLevel), new AtomicReference<>(minLogLevel));
	}
	
	
	public static Logger standardOut(AbstractLogLevel minLogLevel, SimplePrefixFormat...formats) {
		return printStream(minLogLevel, System.out, formats);
	}
	
	public static Logger standardErr(AbstractLogLevel minLogLevel, SimplePrefixFormat...formats) {
		return printStream(minLogLevel, System.err, formats);
	}
	
	public static Logger printStream(AbstractLogLevel minLogLevel, PrintStream outputStream, SimplePrefixFormat...formats) {
		Objects.requireNonNull(minLogLevel, "'minLogLevel' parameter must not be null");
		Objects.requireNonNull(outputStream, "'outputStream' parameter must not be null");
		
		return new Logger(DefaultLoggerDelegates.forFormattedSingleStream(outputStream, formats), new AtomicReference<>(minLogLevel));
	}
	
	public static Logger standardDynamic(AbstractLogLevel minLogLevel, AbstractLogLevel stderrLevel, SimplePrefixFormat...formats) {
		Objects.requireNonNull(minLogLevel, "'minLogLevel' parameter must not be null");
		Objects.requireNonNull(stderrLevel, "'stderrLevel' parameter must not be null");
		
		return new Logger(DefaultLoggerDelegates.forFormattedDualStream(System.out, System.err, stderrLevel, formats), new AtomicReference<>(minLogLevel));
	}
	
	public static Logger derive(Logger base, UnaryOperator<SimplePrefixFormat[]> operator) {
		Objects.requireNonNull(base, "'base' parameter must not be null");
		Objects.requireNonNull(operator, "'operator' parameter must not be null");
		
		LoggerDelegate del = base.delegate;
		final LoggerDelegate newLD;
		if(del instanceof FormattingLoggerDelegate) {
			FormattingLoggerDelegate del2 = (FormattingLoggerDelegate) del;
			SimplePrefixFormat[] newFormat = operator.apply(del2.prefixes);
			newLD = new FormattingLoggerDelegate(del2.base, newFormat);
		} else {
			SimplePrefixFormat[] newFormat = operator.apply(new SimplePrefixFormat[0]);
			newLD = new FormattingLoggerDelegate(del, newFormat);
		}
		return new Logger(newLD, base.cutoffLevel);
	}
	
	public static Logger customDelegate(AbstractLogLevel minLogLevel, Logger.LoggerDelegate loggerDelegate) {
		Objects.requireNonNull(minLogLevel, "'minLogLevel' parameter must not be null");
		Objects.requireNonNull(loggerDelegate, "'loggerDelegate' parameter must not be null");
		
		return new Logger(loggerDelegate, new AtomicReference<>(minLogLevel));
	}
}
