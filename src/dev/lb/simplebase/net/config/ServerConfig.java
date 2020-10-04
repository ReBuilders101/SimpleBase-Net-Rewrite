package dev.lb.simplebase.net.config;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.ServerInfoRequest;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.annotation.ValueType;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.manager.NetworkManagerServer;
import dev.lb.simplebase.net.packet.Packet;

/**
 * A {@link CommonConfig} implementation that stores all configuration settings applicable to a {@link NetworkManagerServer}.
 * <h2>equals and hashCode</h2><p>
 * The info packet factory ({@link #getServerInfoPacket()} is stored as a functional interface. Because the
 * {@link BiFunction} interface does not implement {@code equals} and {@code hashCode()}, {@code ServerConfig} instances
 * might not be equal despite their factories <i>referring</i> to the same method.<br>
 * Because {@link #clone()} creates a flat copy, a cloned instance will still be equal to the original object. 
 * </p><p>
 * The following is copied from the {@code CommonConfig} class description and also applies to this class: 
 * </p>
 * <hr>
 * <h2>Thread safety</h2>
 * <p>
 * The {@link CommonConfig} class and both subclasses included with the API ({@link ClientConfig} and {@link ServerConfig}) are all threadsafe.
 * If the synchronization object is the {@code CommonConfig} object itself. {@code get...()} methods are single reads and never acquire the lock,
 * as those operations will be executed frequently while the program is running. Getters do not require locking because it is impossible to leave
 * a {@code CommonConfig} object in an invalid state.<br>
 * <b>Mutating a config object on multiple threads should not be necessary:</b> Despite being possible, it is rarely necessary or appropiate to
 * modify the config on more than one thread. It is best practise to initialize all values and then <i>lock</i> the config object to prevent further mutation.
 * </p>
 * <h2>Locking Behavior</h2>
 * <p>
 * To ensure that config objects and their properties can be cached anywhere in a {@link NetworkManagerCommon} and related classes the {@link CommonConfig}
 * class includes a special <i>locking</i> mechanism. After a {@code CommonConfig} instance has been locked, all attempts to set a config value
 * will result in an {@link IllegalStateException}. A locked config can never be unlocked, but a new instance that has the same values without being locked can
 * be created using the copy constructor. Config objects can be locked by calling {@link #lock()}.
 * </p>
 * <h2>Comparison and Cloning</h2>
 * <p>
 * The {@link #equals(Object)} method compares all configured values in the two instances. <b>It does not consider the <i>locked</i> state of either config.</b>
 * A locked and a different unlocked {@code CommonConfig} object are considered equal as long as they store the same configuration values and produce equally configured
 * network managers. To comply with the contract for the {@code equals} and {@code hashCode} methods, {@link #hashCode()} also does not consider the <i>locked</i> state
 * when calculating the hash.<br>
 * The locked state will be copied when using the {@link #clone()} method.
 */
@ValueType
@Threadsafe
public class ServerConfig extends CommonConfig {
	
	/**
	 * Can be used in {@link #setServerInfoPacket(InfoPacketFactory)} to indicate that the server does not
	 * provide an info packet. In that case, the server cannot be found using {@link ServerInfoRequest}s LAN scanner.
	 */
	public static final InfoPacketFactory NO_SERVER_INFO = new NoPacketFactory();
	
	private static final boolean REGISTER_INTERNAL_SERVER = true;
	private static final InfoPacketFactory SERVER_INFO_FACTORY = NO_SERVER_INFO;
	private static final ServerType SERVER_TYPE_DEFAULT = ServerType.DEFAULT;
	
	
	private boolean registerInternalServer;
	private ServerType serverType;
	private InfoPacketFactory serverInfoFactory;
	
	/**
	 * <p>
	 * Creates a new {@link ServerConfig} instance with default configuration values. The instance will not be <i>locked</i>.
	 * </p> 
	 * <table>
	 * <caption>Config option defaults:</caption>
	 * <tr><th>Getter method name</th><th>Initial value</th></tr>
	 * <tr><td>{@link #getUseHandlerThread()}</td><td>{@code true}<td></tr>
	 * <tr><td>{@link #getPacketBufferInitialSize()}</td><td>{@code 4096}</td></tr>
	 * <tr><td>{@link #getConnectionCheckTimeout()}</td><td>{@code 5000} [ms]</td></tr>
	 * <tr><td>{@link #getGlobalConnectionCheck()}</td><td>{@code false}</td></tr>
	 * <tr><td>{@link #getCompressionSize()}</td><td>{@link #DISABLE_COMPRESSION}</td></tr>
	 * <tr><td>{@link #getDatagramPacketMaxSize()}</td><td>{@code 4096}</td></tr>
	 * <tr><td>{@link #getUseEncoderThreadPool()}</td><td>{@code true}</td></tr>
	 * <tr><td>{@link #getUseDecoderThreadPool()}</td><td>{@code true}</td></tr>
	 * <tr><td>{@link #getRegisterInternalServer()}</td><td>{@code true}</td></tr>
	 * <tr><td>{@link #getServerType()}</td><td>{@link ServerType#DEFAULT}</td></tr>
	 * <tr><td>{@link #getServerInfoPacket()}</td><td>{@link #NO_SERVER_INFO}</td></tr>
	 * </table>
	 */
	public ServerConfig() {
		super();
		this.registerInternalServer = REGISTER_INTERNAL_SERVER;
		this.serverInfoFactory = SERVER_INFO_FACTORY;
		this.serverType = SERVER_TYPE_DEFAULT;
	}
	
	/**
	 * Creates a new {@link ServerConfig} instance that copies all configuration values from a different {@link CommonConfig} object.
	 * The created instance will not be <i>locked</i>.<br>
	 * The constructor will synchronize on the template object while copying values to prevent concurrent modification.
	 * <p>
	 * This constructor will only copy any configuration values that are present in the {@code CommonConfig} class, even
	 * when the template's runtime type is {@code ServerConfig}. To fully copy a {@code ServerConfig}, use the correct constructor
	 * {@link #ServerConfig(ServerConfig)}.
	 * </p>
	 * @param template The old {@link CommonConfig} that holds the configuration values
	 */
	public ServerConfig(CommonConfig template) {
		this();
		synchronized (template) {
			this.setUseHandlerThread(template.getUseHandlerThread());
			this.setConnectionCheckTimeout(template.getConnectionCheckTimeout());
			this.setPacketBufferInitialSize(template.getPacketBufferInitialSize());
			this.setGlobalConnectionCheck(template.getGlobalConnectionCheck());
			this.setCompressionSize(template.getCompressionSize());
			this.setUseEncoderThreadPool(template.getUseEncoderThreadPool());
			this.setDatagramPacketMaxSize(template.getDatagramPacketMaxSize());
			this.setUseDecoderThreadPool(template.getUseDecoderThreadPool());
		}
	}
	
	/**
	 * Creates a new {@link ServerConfig} instance that copies all configuration values from a different {@code ServerConfig}
	 * The constructor will synchronize on the template object while copying values to prevent concurrent modification.
	 * @param template The old {@link ServerConfig} that holds the configuration values
	 */
	public ServerConfig(ServerConfig template) {
		this();
		synchronized (template) {
			this.setUseHandlerThread(template.getUseHandlerThread());
			this.setConnectionCheckTimeout(template.getConnectionCheckTimeout());
			this.setPacketBufferInitialSize(template.getPacketBufferInitialSize());
			this.setGlobalConnectionCheck(template.getGlobalConnectionCheck());
			this.setCompressionSize(template.getCompressionSize());
			this.setUseEncoderThreadPool(template.getUseEncoderThreadPool());
			this.setDatagramPacketMaxSize(template.getDatagramPacketMaxSize());
			this.setUseDecoderThreadPool(template.getUseDecoderThreadPool());
			this.setRegisterInternalServer(template.getRegisterInternalServer());
			this.setServerType(template.getServerType());
			this.setServerInfoPacket(template.getServerInfoPacket());
		}
	}
	
	/**
	 * Determines whether the {@link NetworkManagerServer} will be useable as an application-internal server.
	 * <p>
	 * If {@code true}, it can be connected to using a {@link NetworkID} with the {@link NetworkIDFunction#INTERNAL}
	 * and will be listed in {@link NetworkManager#getInternalServers()}.
	 * </p>
	 * <p>
	 * If the {@code NetworkID} used to create the server implements {@link NetworkIDFunction#INTERNAL} or the
	 * {@link ServerType} used is {@link ServerType#INTERNAL}, 
	 * this option <b>must</b> be enabled. For all other {@code NetworkID}s and {@code ServerType}s, this is optional
	 * </p>
	 * @return Whether the server will be useable as an application-internal server
	 */
	public boolean getRegisterInternalServer() {
		return registerInternalServer;
	}
	
	/**
	 * Sets whether the {@link NetworkManagerServer} will be useable as an application-internal server.
	 * <p>
	 * If {@code true}, it can be connected to using a {@link NetworkID} with the {@link NetworkIDFunction#INTERNAL}
	 * and will be listed in {@link NetworkManager#getInternalServers()}.
	 * </p>
	 * <p>
	 * If the {@code NetworkID} used to create the server implements {@link NetworkIDFunction#INTERNAL} or the
	 * {@link ServerType} used is {@link ServerType#INTERNAL}, 
	 * this option <b>must</b> be enabled. For all other {@code NetworkID}s and {@code ServerType}s, this is optional
	 * </p>
	 * @param value The new value for this config option
	 * @return {@code this}
	 * @throws IllegalStateException When this config object is locked
	 */
	public synchronized ServerConfig setRegisterInternalServer(boolean value) throws IllegalStateException {
		checkLocked();
		this.registerInternalServer = value;
		return this;
	}
	
	/**
	 * The type of server that will be created from this config.
	 * <p>
	 * Clients that want to connect to the server must use a compatibe {@link ConnectionType} in their config
	 * See the {@link ServerType} documentation to find a list of compatible server and connection types.
	 * </p>
	 * @return The type of the server created from this config
	 */
	public ServerType getServerType() {
		return serverType;
	}
	
	/**
	 * Sets the type of server that will be created from this config.
	 * <p>
	 * Clients that want to connect to the server must use a compatibe {@link ConnectionType} in their config
	 * See the {@link ServerType} documentation to find a list of compatible server and connection types.
	 * </p>
	 * @param value The new value for this config option
	 * @return {@code this}
	 * @throws IllegalStateException When this config object is locked
	 * @throws NullPointerException When {@code value} is {@code null}
	 */
	public synchronized ServerConfig setServerType(ServerType value) throws IllegalStateException {
		checkLocked();
		Objects.requireNonNull(value, "'value' for ServerType must not be null");
		this.serverType = value;
		return this;
	}
	
	/**
	 * The functional interface that represents the method creating a server info packet.
	 * <p>
	 * The server info packet is an application-defined {@link Packet} that can be queried from a
	 * server without establishing a {@link NetworkConnection}. This can be used by {@link ServerInfoRequest}
	 * to find servers in the LAN network or retrieve server descriptions to display in a server list
	 * </p>
	 * <p>
	 * If no packet can be created, the functional method will return {@code null}. The factory itself will
	 * never be {@code null}.<br>
	 * If info packets are disabled completely, the returned factory will be {@link #NO_SERVER_INFO} and
	 * return {@code true} when tested with {@link InfoPacketFactory#isDisabled(InfoPacketFactory)}.
	 * </p>
	 * @return The functional interface that creates server info packets
	 */
	public InfoPacketFactory getServerInfoPacket() {
		return serverInfoFactory;
	}
	
	/**
	 * Sets the functional interface that represents the method creating a server info packet.
	 * <p>
	 * The server info packet is an application-defined {@link Packet} that can be queried from a
	 * server without establishing a {@link NetworkConnection}. This can be used by {@link ServerInfoRequest}
	 * to find servers in the LAN network or retrieve server descriptions to display in a server list
	 * </p>
	 * <p>
	 * If no packet can be created, the functional method should return {@code null}. The factory itself must
	 * never be {@code null}.<br>
	 * <b>If info packets should be disabled completely, set the config value to {@link #NO_SERVER_INFO} and
	 * not to {@code null}.</b>
	 * </p>
	 * @param value The new value for this config option
	 * @return {@code this}
	 * @throws IllegalStateException When this config object is locked
	 */
	public synchronized ServerConfig setServerInfoPacket(InfoPacketFactory value) throws IllegalStateException {
		checkLocked();
		this.serverInfoFactory = Objects.requireNonNull(value, "null is not a valid InfoPacketFactory");
		return this;
	}

	@Override
	public synchronized ServerConfig setDatagramPacketMaxSize(int size) {
		return (ServerConfig) super.setDatagramPacketMaxSize(size);
	}

	@Override
	public synchronized ServerConfig setUseEncoderThreadPool(boolean value) {
		return (ServerConfig) super.setUseEncoderThreadPool(value);
	}

	@Override
	public synchronized ServerConfig setUseDecoderThreadPool(boolean value) {
		return (ServerConfig) super.setUseDecoderThreadPool(value);
	}

	@Override
	public synchronized ServerConfig setUseThreadPools(boolean encodeAndDecode) {
		return (ServerConfig) super.setUseThreadPools(encodeAndDecode);
	}

	@Override
	public synchronized ServerConfig setCompressionSize(int minPacketSize) {
		return (ServerConfig) super.setCompressionSize(minPacketSize);
	}

	@Override
	public synchronized ServerConfig setConnectionCheckTimeout(int value) {
		return (ServerConfig) super.setConnectionCheckTimeout(value);
	}

	@Override
	public synchronized ServerConfig setUseHandlerThread(boolean value) {
		return (ServerConfig) super.setUseHandlerThread(value);
	}

	@Override
	public synchronized ServerConfig setPacketBufferInitialSize(int value) {
		return (ServerConfig) super.setPacketBufferInitialSize(value);
	}

	@Override
	public synchronized ServerConfig setGlobalConnectionCheck(boolean value) {
		return (ServerConfig) super.setGlobalConnectionCheck(value);
	}

	@Override
	public synchronized ServerConfig clone() {
		return (ServerConfig) super.clone();
	}

	@Override
	public synchronized ServerConfig copy() {
		if(isLocked()) {
			return this;
		} else {
			return clone();
		}
	}
	
	@Override
	public String toString() {
		return "ServerConfig [registerInternalServer=" + registerInternalServer
				+ ", serverType=" + serverType + ", serverInfoFactory=" + serverInfoFactory
				+ ", getDatagramPacketMaxSize()=" + getDatagramPacketMaxSize() + ", getUseEncoderThreadPool()="
				+ getUseEncoderThreadPool() + ", getUseDecoderThreadPool()=" + getUseDecoderThreadPool()
				+ ", getCompressionSize()=" + getCompressionSize() + ", getConnectionCheckTimeout()="
				+ getConnectionCheckTimeout() + ", getUseManagedThread()=" + getUseHandlerThread()
				+ ", getPacketBufferInitialSize()=" + getPacketBufferInitialSize() + ", getGlobalConnectionCheck()="
				+ getGlobalConnectionCheck() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(registerInternalServer, serverInfoFactory, serverType);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof ServerConfig)) {
			return false;
		}
		ServerConfig other = (ServerConfig) obj;
		return registerInternalServer == other.registerInternalServer
				&& Objects.equals(serverInfoFactory, other.serverInfoFactory) && serverType == other.serverType;
	}
	
	/**
	 * A {@link FunctionalInterface} that represents a method creating server info packets.
	 * @see ServerConfig#getServerInfoPacket()
	 */
	@FunctionalInterface
	public static interface InfoPacketFactory {
		
		/**
		 * The functional method of 
		 * @param server The server for which the packet is requested
		 * @param requestSource The source {@link InetSocketAddress} of the request. If the request did not come over the
		 * network, the source is empty
		 * @return A {@link Packet}, or {@code null} if no packet could be created for this request
		 */
		public Packet createPacket(NetworkManagerServer server, Optional<InetSocketAddress> requestSource);
		
		/**
		 * Determines whether the tested {@link InfoPacketFactory} means that info packets are disabled completely for the server.
		 * @param factory The {@link InfoPacketFactory} to test
		 * @return Whether using this factory disables info packets completely 
		 */
		public static boolean isDisabled(InfoPacketFactory factory) {
			return factory == null || factory instanceof NoPacketFactory;
		}
	}

	private static final class NoPacketFactory implements InfoPacketFactory {

		@Override
		public Packet createPacket(NetworkManagerServer server, Optional<InetSocketAddress> requestSource) {
			return null;
		}
		
	}
}
