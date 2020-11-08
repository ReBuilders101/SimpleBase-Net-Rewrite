package dev.lb.simplebase.net;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import dev.lb.simplebase.net.annotation.StaticType;
import dev.lb.simplebase.net.config.ClientConfig;
import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.config.ConnectionType;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.config.ServerType;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.AbstractLogLevel;
import dev.lb.simplebase.net.log.Logger;
import dev.lb.simplebase.net.log.LogLevel;
import dev.lb.simplebase.net.log.LogManager;
import dev.lb.simplebase.net.log.SimplePrefixFormat;
import dev.lb.simplebase.net.manager.ChannelNetworkManagerServer;
import dev.lb.simplebase.net.manager.InternalNetworkManagerServer;
import dev.lb.simplebase.net.manager.NetworkManagerClient;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.manager.NetworkManagerServer;
import dev.lb.simplebase.net.manager.SocketNetworkManagerServer;

/**
 * <p>
 * The {@link NetworkManager} class provides static methods to create client and server managers.
 * </p><p>
 * It also offers methods related to logging and application behavior such as cleanup tasks
 * </p>
 */
@StaticType
public final class NetworkManager {
	
	private NetworkManager() {/*No instantiation possible, contains only static utility methods*/}
	
	//LOGGERS ###############################################################################
	
	/**
	 * The {@link Logger} used by the API to give information to the user.<br>
	 * Call {@link Logger#setLogLevel(AbstractLogLevel)} to set detail level.
	 */
	private static final Logger NET_LOG = 
			LogManager.standardDynamic(LogLevel.LOWEST, LogLevel.ERROR,
					SimplePrefixFormat.forString("Net-Simplebase"),
					SimplePrefixFormat.forLogLevel(),
					SimplePrefixFormat.forTime(DateTimeFormatter.ISO_LOCAL_TIME),
					SimplePrefixFormat.forThread());
//			
//			Loggers.printSysOut(LogLevel.METHOD,
//			Formatter.getPrefix(
//					Formatter.getStaticText("Net-Simplebase"),
//					Formatter.getLogLevel(),
//					Formatter.getCurrentTime(),
//					Formatter.getThreadName()),
//			Formatter.getDefault());
	
	private static final Map<String, Logger> existingDelegateLoggers = new HashMap<>();
	static final Logger LOGGER = getModuleLogger("net-core");
	
	/**
	 * Gets a logger for a custom subsystem. Loggers are cached, and re-calling this method with the same
	 * module name gives the same logger. All returned loggers share the same log level and output destination.
	 * @param moduleName The name of the subsystem. Will appear as a prefix for every log message
	 * @return The custom logger
	 */
	public static Logger getModuleLogger(String moduleName) {
		synchronized (existingDelegateLoggers) {
			return existingDelegateLoggers.computeIfAbsent(moduleName, 
					(name) -> LogManager.derive(NET_LOG, (list) -> {
						List<SimplePrefixFormat> newFormat = new ArrayList<>();
						newFormat.add(SimplePrefixFormat.forString(moduleName));
						newFormat.addAll(list);
						return newFormat;
					}
				)
			);
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
	
	//CLEANUP #################################################################################
	private static final List<Runnable> cleanUpTasks = new ArrayList<>();
	
	
	private static final MethodType timeSourceType;
	private static final MethodHandle defaultTimeSource;
	private static volatile MethodHandle timeSource;
	
	static {
		timeSourceType = MethodType.methodType(long.class);
		try {
			defaultTimeSource = MethodHandles.lookup().findStatic(System.class, "currentTimeMillis", timeSourceType);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException("Cannot find System.currentTimeMillis()");
		}
		timeSource = defaultTimeSource;
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				synchronized (cleanUpTasks) {
					if(cleanUpTasks.size() > 0) {
						LOGGER.warning("Cleanup tasks have not been run. Please call NetworkManager.cleanUp() before exiting the program");
						cleanup();
					}
				}
		}, "Net-Simplebase-Cleanup"));
	}
	
	/**
	 * Register a {@link Runnable} that will be run when {@link #cleanup()} is called. Can be used to
	 * dipose of resources automatically when closing the API.
	 * @param task The task to run when cleaning up
	 */
	public static void addCleanupTask(Runnable task) {
		synchronized (cleanUpTasks) {
			cleanUpTasks.add(task);
		}
	}
	
	/**
	 * <b>Should be called before the application using this API exits.</b>
	 * <p>
	 * Disposes of all resources held by the API,
	 * stops all active servers after terminating their connections
	 * and ends all non-daemon threads created by the API.
	 * </p><p>
	 * If not called before the application exits, it will run automatically using a shutdown hook,
	 * but relying on this behavior is discouraged and will be logged as a warning.
	 * </p>
	 */
	public static void cleanup() {
		synchronized (cleanUpTasks) {
			LOGGER.info("Cleanup: running %d tasks", cleanUpTasks.size());
			
			cleanUpTasks.forEach(Runnable::run);
			cleanUpTasks.clear();
			
			//This is the very last thing in case a cleanup tasks still requires the timer
			GlobalTimer.cleanup();
			LOGGER.info("Cleanup: completed; task list cleared");
		}
	}
	
	//UTILITIES ################################################################################
	
	/**
	 * A list of all {@link NetworkManagerServer} that are currently available for internal
	 * connections.
	 * @return An immutable {@link Set} of registered internal servers
	 */
	public static Set<NetworkManagerServer> getInternalServers() {
		return InternalServerProvider.getInternalServers();
	}
	
	
	/**
	 * A consistent time source. Defaults to
	 * {@link System#currentTimeMillis()}, but can be swapped for a faster or more precise time source if necessary
	 * (See 'granularity' in the docs)
	 * @return The current time in milliseconds. The value will be positive, but there are no guarantees to what point
	 * in time the {@code 0} value represents (This may be an Epoch date, or e.g. the start time of the program).
	 */
	public static long getClockMillis() {
		try {
			return (long) timeSource.invokeExact();
		} catch (Throwable e) {
			throw new RuntimeException("Time source threw exception: ", e);
		}
	}
	
	/**
	 * Can be used to replace the APIs time source.
	 * <b>Protected as this is an experimental feature.</b>
	 * <p>
	 * The {@link MethodHandle} must accept no parameters and return a value of type {@code long} (Descriptor: '()J').
	 * </p><p>
	 * If the supplied {@code MethodHandle} is {@code null}, the default source ({@link System#currentTimeMillis()})
	 * will be used as a fallback.
	 * </p>
	 * @param newTimeSource The {@link MethodHandle} to the new time source, or {@code null}
	 * @throws IllegalArgumentException When the {@link MethodType} of the handle is incorrect
	 */
	protected static void setTimeSource(MethodHandle newTimeSource) {
		if(newTimeSource == null) {
			timeSource = defaultTimeSource;
		} else {
			if(newTimeSource.type().equals(timeSourceType)) {
				timeSource = newTimeSource;
			} else {
				throw new IllegalArgumentException("MethodHandle has incorrect MethodType: ()J required");
			}
		}
	}
	
	/**
	 * Sets the timer period for the task that calls {@link NetworkManagerCommon#updateConnectionStatus()}
	 * if {@link CommonConfig#getGlobalConnectionCheck()} was enabled.
	 * @param period The time period in the given unit
	 * @param unit The time unit for the period
	 */
	public static void setConnectionCheckTimer(long period, TimeUnit unit) {
		long millis = unit.toMillis(period);
		//Adjust rounding errors to avoid unexpected error messages
		if(millis == 0 && period > 0) millis = 1;
		if(millis <= 0) throw new IllegalArgumentException("Period must be greater than 0");
		GlobalTimer.setManagerTimeout(millis);
	}
	
	//MANAGER FACTORIES #######################################################################
	
	private static <T extends NetworkManagerCommon> T register(T instance) {
		if(instance == null) return null;
		addCleanupTask(instance::cleanUp);
		return instance;
	}
	
	/**
	 * Creates a new {@link NetworkManagerClient} instance.<br>
	 * All config values are the defaults listed in {@link ClientConfig#ClientConfig()}.
	 * @param clientLocal The local address of the client
	 * @param serverRemote The remote address of the server to connect to
	 * @return The created {@link NetworkManagerClient}, or {@code null} if an error occurred during creation
	 * @throws NullPointerException When any of the paramters is {@code null}
	 */
	public static NetworkManagerClient createClient(NetworkID clientLocal, NetworkID serverRemote) {
		return createClient(clientLocal, serverRemote, new ClientConfig());
	}
	
	/**
	 * Creates a new {@link NetworkManagerClient} instance with a custom configuration.
	 * @param clientLocal The local address of the client
	 * @param serverRemote The remote address of the server to connect to
	 * @param config The {@link ClientConfig} with additional settings for the manager
	 * @return The created {@link NetworkManagerClient}, or {@code null} if an error occurred during creation
	 * @throws NullPointerException When any of the paramters is {@code null}
	 */
	public static NetworkManagerClient createClient(NetworkID clientLocal, NetworkID serverRemote, ClientConfig config) {
		Objects.requireNonNull(clientLocal, "'clientLocal' parameter must not be null");
		Objects.requireNonNull(serverRemote, "'serverRemote' parameter must not be null");
		Objects.requireNonNull(config, "'config' parameter must not be null");
		
		final ConnectionType actualType = ConnectionType.resolve(config.getConnectionType(), serverRemote);
		final ClientConfig copiedConfig = new ClientConfig(config);
		copiedConfig.setConnectionType(actualType);
		copiedConfig.lock();
		
		return register(new NetworkManagerClient(clientLocal, serverRemote, copiedConfig));
	}
	
	/**
	 * Creates a new {@link NetworkManagerServer} instance.<br>
	 * All config values are the defaults listed in {@link ServerConfig#ServerConfig()}.
	 * @param serverLocal The address for the server to bind to
	 * @return The created {@link NetworkManagerServer}, or {@code null} if an error occurred during creation
	 * @throws NullPointerException When any of the paramters is {@code null}
	 */
	public static NetworkManagerServer createServer(NetworkID serverLocal) {
		return createServer(serverLocal, new ServerConfig());
	}
	
	/**
	 * Creates a new {@link NetworkManagerServer} instance.<br>
	 * All config values are the defaults listed in {@link ServerConfig#ServerConfig()}.
	 * @param serverLocal The address for the server to bind to
	 * @param config The {@link ServerConfig} with additional settings for the manager
	 * @return The created {@link NetworkManagerServer}, or {@code null} if an error occurred during creation
	 * @throws NullPointerException When any of the paramters is {@code null}
	 * @throws IllegalArgumentException When there is a mismatch between the config's {@link ServerType} and the used {@link NetworkID}
	 */
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
			return register(new InternalNetworkManagerServer(serverLocal, copiedConfig));
		case TCP_IO:
		case UDP_IO:
		case COMBINED_IO:
			try {
				return register(new SocketNetworkManagerServer(serverLocal, copiedConfig));
			} catch (IOException e) {
				LOGGER.error("Cannot create SocketNetworkManagerServer", e);
				return null;
			}
		case TCP_NIO:
		case UDP_NIO:
		case COMBINED_NIO:
			try {
				return register(new ChannelNetworkManagerServer(serverLocal, copiedConfig));
			} catch (IOException e) {
				LOGGER.error("Cannot create ChannelNetworkManagerServer", e);
				return null;
			}
		default:
			throw new IllegalArgumentException("Invalid server type: " + actualType);
		}
	}
}
