package dev.lb.simplebase.net.config;

import java.util.Objects;

import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.annotation.ValueType;
import dev.lb.simplebase.net.manager.NetworkManagerClient;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.packet.handler.PacketHandler;

/**
 * A {@link CommonConfig} implementation that stores all configuration settings applicable to a {@link NetworkManagerClient}.
 * <p>
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
public class ClientConfig extends CommonConfig {
	
	/**
	 * Can be used in {@link #setCustomData(Object)} to indicate that the client's connection
	 * will not have a custom object attached to it. The connection's custom object will be {@code null}.
	 */
	public static final Object NO_CUSTOM_DATA = null;
	
	private static final ConnectionType CONNECTION_TYPE = ConnectionType.DEFAULT;
	private static final Object CUSTOM_DATA = NO_CUSTOM_DATA;
	
	private ConnectionType connectionType;
	private Object customData;
	
	/**
	 * <p>
	 * Creates a new {@link CommonConfig} instance with default configuration values. The instance will not be <i>locked</i>.
	 * </p>
	 * <table>
	 * <caption>Config option defaults:</caption>
	 * <tr><th>Getter method name</th><th>Initial value</th></tr>
	 * <tr><td>{@link #getUseHandlerThread()}</td><td>{@code true}<td></tr>
	 * <tr><td>{@link #getPacketBufferInitialSize()}</td><td>{@code 4096}</td></tr>
	 * <tr><td>{@link #getConnectionCheckTimeout()}</td><td>{@code 5000} [ms]</td></tr>
	 * <tr><td>{@link #getGlobalConnectionCheck()}</td><td>{@code false}</td></tr>
	 * <tr><td>{@link #getCompressionSize()}</td><td>{@link CommonConfig#DISABLE_COMPRESSION}</td></tr>
	 * <tr><td>{@link #getDatagramPacketMaxSize()}</td><td>{@code 4096}</td></tr>
	 * <tr><td>{@link #getUseEncoderThreadPool()}</td><td>{@code true}</td></tr>
	 * <tr><td>{@link #getUseDecoderThreadPool()}</td><td>{@code true}</td></tr>
	 * <tr><td>{@link #getConnectionType()}</td><td>{@link ConnectionType#DEFAULT}</td></tr>
	 * <tr><td>{@link #getCustomData()}</td><td>{@link #NO_CUSTOM_DATA}</td></tr>
	 * </table>
	 */
	public ClientConfig() {
		super();
		this.connectionType = CONNECTION_TYPE;
		this.customData = CUSTOM_DATA;
	}
	
	/**
	 * Creates a new {@link ClientConfig} instance that copies all configuration values from a different {@link CommonConfig} object.
	 * The created instance will not be <i>locked</i>.<br>
	 * The constructor will synchronize on the template object while copying values to prevent concurrent modification.
	 * <p>
	 * This constructor will only copy any configuration values that are present in the {@code CommonConfig} class, even
	 * when the template's runtime type is {@code ClientConfig}. To fully copy a {@code ClientConfig}, use the correct constructor
	 * {@link #ClientConfig(ClientConfig)}.
	 * </p>
	 * @param template The old {@link CommonConfig} that holds the configuration values
	 */
	public ClientConfig(CommonConfig template) {
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
	 * Creates a new {@link ClientConfig} instance that copies all configuration values from a different {@code ClientConfig} object.
	 * The created instance will not be <i>locked</i>.<br>
	 * The constructor will synchronize on the template object while copying values to prevent concurrent modification.
	 * @param template The old {@link ClientConfig} that holds the configuration values
	 */
	public ClientConfig(ClientConfig template) {
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
			this.setConnectionType(template.getConnectionType());
			this.setCustomData(template.getCustomData());
		}
	}
	
	/**
	 * The type of network connection that will be created when connecting the client to the server.
	 * <p>
	 * To successfully connect to a server, the server must have a compatible {@link ServerType}.
	 * See the {@link ConnectionType} documentation to find a list of compatible server and connection types
	 * </p>
	 * @return The {@link ConnectionType} that the client will make to the server
	 */
	public ConnectionType getConnectionType() {
		return connectionType;
	}
	
	/**
	 * Sets the type of network connection that will be created when connecting the client to the server.
	 * <p>
	 * To successfully connect to a server, the server must have a compatible {@link ServerType}.
	 * See the {@link ConnectionType} documentation to find a list of compatible server and connection types
	 * </p>
	 * @param value The new value for this config option
	 * @return {@code this}
	 * @throws IllegalStateException When this config object is locked
	 */
	public synchronized ClientConfig setConnectionType(ConnectionType value) throws IllegalStateException {
		checkLocked();
		this.connectionType = value;
		return this;
	}
	
	/**
	 * A user-defined custom object that will be associated with the connection the client makes to the server.
	 * <p>
	 * Here, an object can be stored to be later retrieved in a {@link PacketHandler} using the {@link PacketContext#getCustomData()}
	 * method. Set to {@link #NO_CUSTOM_DATA} to store no object.
	 * </p>
	 * @return The user-defined custom object for the server connection
	 */
	public Object getCustomData() {
		return customData;
	}
	
	/**
	 * Sets the user-defined custom object that will be associated with the connection the client makes to the server.
	 * <p>
	 * Here, an object can be stored to be later retrieved in a {@link PacketHandler} using the {@link PacketContext#getCustomData()}
	 * method. Set to {@link #NO_CUSTOM_DATA} to store no object.
	 * </p>
	 * @param value The new value for this config option
	 * @return {@code this}
	 * @throws IllegalStateException When this config object is locked
	 */
	public synchronized ClientConfig setCustomData(Object value) throws IllegalStateException {
		checkLocked();
		this.customData = value;
		return this;
	}

	@Override
	public synchronized ClientConfig setDatagramPacketMaxSize(int size) {
		return (ClientConfig) super.setDatagramPacketMaxSize(size);
	}

	@Override
	public synchronized ClientConfig setUseEncoderThreadPool(boolean value) {
		return (ClientConfig) super.setUseEncoderThreadPool(value);
	}

	@Override
	public synchronized ClientConfig setUseDecoderThreadPool(boolean value) {
		return (ClientConfig) super.setUseDecoderThreadPool(value);
	}

	@Override
	public synchronized ClientConfig setUseThreadPools(boolean encodeAndDecode) {
		return (ClientConfig) super.setUseThreadPools(encodeAndDecode);
	}

	@Override
	public synchronized ClientConfig setCompressionSize(int minPacketSize) {
		return (ClientConfig) super.setCompressionSize(minPacketSize);
	}

	@Override
	public synchronized ClientConfig setConnectionCheckTimeout(int value) {
		return (ClientConfig) super.setConnectionCheckTimeout(value);
	}

	@Override
	public synchronized ClientConfig setUseHandlerThread(boolean value) {
		return (ClientConfig) super.setUseHandlerThread(value);
	}

	@Override
	public synchronized ClientConfig setPacketBufferInitialSize(int value) {
		return (ClientConfig) super.setPacketBufferInitialSize(value);
	}

	@Override
	public synchronized ClientConfig setGlobalConnectionCheck(boolean value) {
		return (ClientConfig) super.setGlobalConnectionCheck(value);
	}

	@Override
	public synchronized ClientConfig clone() {
		return (ClientConfig) super.clone();
	}

	@Override
	public synchronized ClientConfig copy() {
		if(isLocked()) {
			return this;
		} else {
			return clone();
		}
	}
	
	@Override
	public String toString() {
		return "ClientConfig [connectionType=" + connectionType + ", customData=" + customData
				+ ", getDatagramPacketMaxSize()=" + getDatagramPacketMaxSize() + ", getUseEncoderThreadPool()="
				+ getUseEncoderThreadPool() + ", getUseDecoderThreadPool()=" + getUseDecoderThreadPool()
				+ ", getCompressionSize()=" + getCompressionSize() + ", getConnectionCheckTimeout()="
				+ getConnectionCheckTimeout() + ", getUseManagedThread()=" + getUseHandlerThread()
				+ ", getPacketBufferInitialSize()=" + getPacketBufferInitialSize() + ", getGlobalConnectionCheck()="
				+ getGlobalConnectionCheck() + ", isLocked()=" + isLocked() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(connectionType, customData);
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
		if (!(obj instanceof ClientConfig)) {
			return false;
		}
		ClientConfig other = (ClientConfig) obj;
		return connectionType == other.connectionType && Objects.equals(customData, other.customData);
	}
}
