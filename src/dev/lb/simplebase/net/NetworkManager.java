package dev.lb.simplebase.net;

import java.util.Objects;

import dev.lb.simplebase.net.annotation.StaticType;
import dev.lb.simplebase.net.config.ClientConfig;
import dev.lb.simplebase.net.id.NetworkID;
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
	
	public static NetworkManagerClient createClient(NetworkID clientLocal, NetworkID serverRemote, ClientConfig config) {
		Objects.requireNonNull(clientLocal, "'clientLocal' parameter must not be null");
		Objects.requireNonNull(serverRemote, "'serverRemote' parameter must not be null");
		Objects.requireNonNull(config, "'config' parameter must not be null");
		
		return new NetworkManagerClient(clientLocal, serverRemote, config);
	}
	
	public static NetworkManagerClient createClient(NetworkID clientLocal, NetworkID serverRemote) {
		return createClient(clientLocal, serverRemote, new ClientConfig());
	}
	
	
}
