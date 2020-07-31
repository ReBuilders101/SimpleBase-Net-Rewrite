package dev.lb.simplebase.net;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.StaticType;
import dev.lb.simplebase.net.config.ClientConfig;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.config.ServerType;
import dev.lb.simplebase.net.connection.InternalNetworkConnection;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.AbstractLogLevel;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.log.DelegateFormattableLogger;
import dev.lb.simplebase.net.log.Formatter;
import dev.lb.simplebase.net.log.LogLevel;
import dev.lb.simplebase.net.log.Loggers;
import dev.lb.simplebase.net.log.PrintStreamLogger;
import dev.lb.simplebase.net.manager.NetworkManagerClient;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;

@StaticType
public final class NetworkManager {
	
	private NetworkManager() {/*No instantiation possible, contains only static utility methods*/}
	
	//LOGGERS ###############################################################################
	
	/**
	 * The {@link AbstractLogger} used by the API to give information to the user.<br>
	 * Call {@link AbstractLogger#setLogLevel(AbstractLogLevel)} to set detail level.
	 */
	private static final PrintStreamLogger NET_LOG = Loggers.printSysOut(LogLevel.METHOD,
			Formatter.getPrefix(
					Formatter.getStaticText("Net-Simplebase"),
					Formatter.getLogLevel(),
					Formatter.getCurrentTime(),
					Formatter.getThreadName()),
			Formatter.getDefault());
	
	private static final Map<String, AbstractLogger> existingDelegateLoggers = new HashMap<>();
	static final AbstractLogger LOGGER = getModuleLogger("net-core");
	
	/**
	 * Gets a logger for a custom subsystem. Loggers are cached, and re-calling this method with the same
	 * module name gives the same logger. All returned loggers share the same log level and output destination.
	 * @param moduleName The name of the subsystem. Will appear as a prefix for every log message
	 * @return The custom logger
	 */
	public static AbstractLogger getModuleLogger(String moduleName) {
		synchronized (existingDelegateLoggers) {
			return existingDelegateLoggers.computeIfAbsent(moduleName, 
					(name) -> new DelegateFormattableLogger(NET_LOG, Formatter.getStaticText(name)));
		}
	}
	
	/**
	 * Sets the {@link LogLevel} for all loggers created with {@link #getModuleLogger(String)}.
	 * Messages with a priority lower than this level will not be logged 
	 * @param level The lowest loggable level
	 */
	public static void setLogLevel(AbstractLogLevel level) {
		NET_LOG.setLogLevel(level);
	}
	
	/**
	 * Sets the {@link PrintStream} for all loggers created with {@link #getModuleLogger(String)}.
	 * Messages will pe printed to this stream 
	 * @param stream The new {@link PrintStream}
	 */
	public static void setPrintStream(PrintStream stream) {
		NET_LOG.setPrintStream(stream);
	}
	
	//CLEANUP #################################################################################
	private static final List<Runnable> cleanUpTasks = new ArrayList<>();
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread("Net-Simplebase-Cleanup") {
			@Override
			public void run() {
				synchronized (cleanUpTasks) {
					if(cleanUpTasks.size() > 0) {
						LOGGER.warning("Cleanup tasks have not been run. Please call NetworkManager.cleanUp() before exiting the program");
						cleanUp();
					}
				}
			}
		});
	}
	
	public static void addCleanUpTask(Runnable task) {
		synchronized (cleanUpTasks) {
			cleanUpTasks.add(task);
		}
	}
	
	public static void cleanUp() {
		synchronized (cleanUpTasks) {
			LOGGER.info("Cleanup: running %d tasks", cleanUpTasks.size());
			cleanUpTasks.forEach(Runnable::run);
			cleanUpTasks.clear();
			LOGGER.info("Cleanup: completed; task list cleared");
		}
	}
	
	//UTILITIES ################################################################################
	
	public static Stream<NetworkManagerServer> getInternalServers() {
		return InternalServerManager.getInternalServers();
	}
	
	
	/**
	 * A consistent time source. Defaults to
	 * {@link System#currentTimeMillis()}, but can be swapped for a faster or more preceise time source if necessary
	 * (See 'granularity' in the docs)
	 * @return The current time in milliseconds. The value will be positive, but there are no guarantees to what point
	 * in time the {@code 0} value represents (This may be an Epoch date, or e.g. the start time of the program).
	 */
	public static long getClockMillis() {
		return System.currentTimeMillis();
	}
	
	//MANAGER FACTORIES #######################################################################
	
	public static NetworkManagerClient createClient(NetworkID clientLocal, NetworkID serverRemote) {
		return createClient(clientLocal, serverRemote, new ClientConfig());
	}
	
	public static NetworkManagerClient createClient(NetworkID clientLocal, NetworkID serverRemote, ClientConfig config) {
		Objects.requireNonNull(clientLocal, "'clientLocal' parameter must not be null");
		Objects.requireNonNull(serverRemote, "'serverRemote' parameter must not be null");
		Objects.requireNonNull(config, "'config' parameter must not be null");
		
		return new NetworkManagerClient(clientLocal, serverRemote, config);
	}
	
	public static NetworkManagerServer createServer(NetworkID serverLocal) {
		return createServer(serverLocal, new ServerConfig());
	}
	
	public static NetworkManagerServer createServer(NetworkID serverLocal, ServerConfig config) {
		Objects.requireNonNull(serverLocal, "'serverLocal' parameter must not be null");
		Objects.requireNonNull(config, "'config' parameter must not be null");
		
		final ServerType actualType = ServerType.resolve(config.getServerType(), serverLocal);
		switch (actualType) {
		case INTERNAL:
			
		default:
			throw new IllegalArgumentException("Invalid server type: " + actualType);
		}
	}

	@Internal
	public static class InternalAccess {
		public static final InternalAccess INSTANCE = new InternalAccess();
		private InternalAccess() {}
		
		public void registerManagerForConnectionStatusCheck(NetworkManagerCommon manager) {
			GlobalConnectionCheck.subscribe(manager);
		}
		
		public void unregisterManagerForConnectionStatusCheck(NetworkManagerCommon manager) {
			GlobalConnectionCheck.unsubscribe(manager);
		}
		
		public InternalNetworkConnection createInternalConnectionPeer(InternalNetworkConnection request) {
			return InternalServerManager.createServerPeer(request);
		};
	}
}
