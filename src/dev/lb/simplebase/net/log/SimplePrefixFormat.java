package dev.lb.simplebase.net.log;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public interface SimplePrefixFormat {

	public Stream<String> getPrefix(AbstractLogLevel level);
	
	
	public static SimplePrefixFormat forLogLevel() {
		return level -> Stream.of("[", level.toString(), "]");
	}
	
	public static SimplePrefixFormat forThread() {
		return level -> Stream.of("[", Thread.currentThread().getName(), "]");
	}
	
	public static SimplePrefixFormat forTime(DateTimeFormatter format) {
		return level -> Stream.of("[", format.format(Instant.now()), "]");
	}
	
	public static SimplePrefixFormat forString(String text) {
		return level -> Stream.of("[", text, "]");
	}
}
