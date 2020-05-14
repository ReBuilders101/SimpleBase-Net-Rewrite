package dev.lb.simplebase.net;

import dev.lb.simplebase.net.annotation.StaticType;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.log.Formatter;
import dev.lb.simplebase.net.log.LogLevel;
import dev.lb.simplebase.net.log.Loggers;

@StaticType
public final class NetworkManager {
	
	private NetworkManager() {/*No instantiation possible, contains only static utility methods*/}
	
	/**
	 * The {@link AbstractLogger} used by the API to give information to the user.<br>
	 * Call {@link AbstractLogger#setLogLevel(AbstractLogLevel)} to set detail level.
	 */
	public static final AbstractLogger NET_LOG = Loggers.printSysOut(LogLevel.METHOD, Formatter.getComplex(
					Formatter.getStaticText("Net-Simplebase"),
					Formatter.getLogLevel(),
					Formatter.getCurrentTime(),
					Formatter.getThreadName(),
					Formatter.getDefault()));
	
	public static void main(String[] args) {
		
	}
	
}
