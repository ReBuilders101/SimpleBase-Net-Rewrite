package dev.lb.simplebase.net;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import dev.lb.simplebase.net.annotation.StaticType;
import dev.lb.simplebase.net.config.ClientConfig;
import dev.lb.simplebase.net.config.ConnectionType;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.config.ServerType;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.AbstractLogLevel;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.log.DelegateFormattableLogger;
import dev.lb.simplebase.net.log.Formatter;
import dev.lb.simplebase.net.log.LogLevel;
import dev.lb.simplebase.net.log.Loggers;
import dev.lb.simplebase.net.log.PrintStreamLogger;
import dev.lb.simplebase.net.manager.ChannelNetworkManagerServer;
import dev.lb.simplebase.net.manager.InternalNetworkManagerServer;
import dev.lb.simplebase.net.manager.NetworkManagerClient;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.manager.NetworkManagerServer;
import dev.lb.simplebase.net.manager.SocketNetworkManagerServer;
import dev.lb.simplebase.net.packet.converter.ByteDeflater;
import dev.lb.simplebase.net.packet.converter.ByteInflater;

/**
 * <p>
 * The {@link NetworkManager} class provides static methods to create client and server managers.
 * </p><p>
 * It also offers methods related to logging and application behavior such as cleanup tasks
 * <p>
 */
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
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				synchronized (cleanUpTasks) {
					if(cleanUpTasks.size() > 0) {
						LOGGER.warning("Cleanup tasks have not been run. Please call NetworkManager.cleanUp() before exiting the program");
						cleanUp();
					}
				}
		}, "Net-Simplebase-Cleanup"));
	}
	
	public static void addCleanUpTask(Runnable task) {
		synchronized (cleanUpTasks) {
			cleanUpTasks.add(task);
		}
	}
	
	public static void cleanUp() {
		synchronized (cleanUpTasks) {
			LOGGER.info("Cleanup: running %d tasks", cleanUpTasks.size());
			//There are some fixed static tasks
			ByteInflater.ZIP_COMPRESSION_PREFIXED.close();
			ByteDeflater.ZIP_COMPRESSION_PREFIXED.close();
			
			cleanUpTasks.forEach(Runnable::run);
			cleanUpTasks.clear();
			LOGGER.info("Cleanup: completed; task list cleared");
		}
	}
	
	//UTILITIES ################################################################################
	
	public static Stream<NetworkManagerServer> getInternalServers() {
		return InternalServerProvider.getInternalServers();
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
	
	private static <T extends NetworkManagerCommon> T register(T instance) {
		if(instance == null) return null;
		addCleanUpTask(instance::cleanUp);
		return instance;
	}
	
	public static NetworkManagerClient createClient(NetworkID clientLocal, NetworkID serverRemote) {
		return createClient(clientLocal, serverRemote, new ClientConfig());
	}
	
	public static NetworkManagerClient createClient(NetworkID clientLocal, NetworkID serverRemote, ClientConfig config) {
		Objects.requireNonNull(clientLocal, "'clientLocal' parameter must not be null");
		Objects.requireNonNull(serverRemote, "'serverRemote' parameter must not be null");
		Objects.requireNonNull(config, "'config' parameter must not be null");
		
		final ConnectionType actualType = ConnectionType.resolve(config.getConnectionType(), serverRemote);
		final ClientConfig copiedConfig = new ClientConfig(config);
		copiedConfig.setConnectionType(actualType);
		copiedConfig.lock();
		
		return register(new NetworkManagerClient(clientLocal, serverRemote, copiedConfig, 0));
	}
	
	public static NetworkManagerServer createServer(NetworkID serverLocal) {
		return createServer(serverLocal, new ServerConfig());
	}
	
	public static NetworkManagerServer createServer(NetworkID serverLocal, ServerConfig config) {
		Objects.requireNonNull(serverLocal, "'serverLocal' parameter must not be null");
		Objects.requireNonNull(config, "'config' parameter must not be null");
		
		//Create actual server type and config
		final ServerType actualType = ServerType.resolve(config.getServerType(), serverLocal);
		final ServerConfig copiedConfig = new ServerConfig(config);
		copiedConfig.setServerType(actualType);
		copiedConfig.lock();
		
		switch (actualType) {
		case INTERNAL:
			return register(new InternalNetworkManagerServer(serverLocal, copiedConfig, 0));
		case TCP_IO:
		case UDP_IO:
		case COMBINED_IO:
			try {
				return register(new SocketNetworkManagerServer(serverLocal, copiedConfig, 0));
			} catch (IOException e) {
				LOGGER.error("Cannot create SocketNetworkManagerServer", e);
				return null;
			}
		case TCP_NIO:
		case UDP_NIO:
		case COMBINED_NIO:
			try {
				return register(new ChannelNetworkManagerServer(serverLocal, copiedConfig, 0));
			} catch (IOException e) {
				LOGGER.error("Cannot create ChannelNetworkManagerServer", e);
				return null;
			}
		default:
			throw new IllegalArgumentException("Invalid server type: " + actualType);
		}
	}
}
