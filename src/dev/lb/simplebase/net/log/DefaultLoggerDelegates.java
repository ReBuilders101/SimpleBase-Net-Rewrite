package dev.lb.simplebase.net.log;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.StaticType;
import dev.lb.simplebase.net.log.Logger.LoggerDelegate;

@Internal
@StaticType
final class DefaultLoggerDelegates {

	static Logger.LoggerDelegate forSingleStream(PrintStream stream) {
		return (level, message, exception) -> {
			stream.println(message);
			if(exception != null) {
				exception.printStackTrace(stream);
			}
		};
	}
	
	static Logger.LoggerDelegate forDualStream(PrintStream lower, PrintStream higher, AbstractLogLevel limit) {
		return (level, message, exception) -> {
			final PrintStream current = level.isAbove(limit) ? higher : lower;
			
			current.println(message);
			if(exception != null) {
				exception.printStackTrace(current);
			}
		};
	}
	
	static Logger.LoggerDelegate forFormattedSingleStream(PrintStream stream, SimplePrefixFormat[] formats) {
		return new FormattingLoggerDelegate(forSingleStream(stream), Arrays.asList(formats));
	}
	
	static Logger.LoggerDelegate forFormattedDualStream(PrintStream lower, PrintStream higher, AbstractLogLevel limit, SimplePrefixFormat[] formats) {
		return new FormattingLoggerDelegate(forDualStream(lower, higher, limit), Arrays.asList(formats));
	}

	static final class FormattingLoggerDelegate implements Logger.LoggerDelegate {
		final Collection<SimplePrefixFormat> prefixes;
		final LoggerDelegate base;
		
		FormattingLoggerDelegate(LoggerDelegate base, Collection<SimplePrefixFormat> prefixes) {
			this.prefixes = Collections.unmodifiableCollection(new ArrayList<>(prefixes));
			this.base = base;
		}
		@Override
		public void logImpl(AbstractLogLevel level, String message, Exception stacktrace) {
			base.logImpl(level, buildPrefix(level, prefixes) + message, stacktrace);
		}
	}
	
	@SuppressWarnings("unused")
	private static String repeat(char c, int length) {
		final char[] array = new char[length];
		Arrays.fill(array, c);
		return new String(array);
	}
	
	private static String buildPrefix(AbstractLogLevel level, Collection<SimplePrefixFormat> format) {
		return format.stream().flatMap(f -> 
			Stream.concat(f.getPrefix(level), Stream.of(" ")))
				.collect(Collectors.joining());
	}
	
}
