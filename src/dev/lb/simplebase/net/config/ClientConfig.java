package dev.lb.simplebase.net.config;

import dev.lb.simplebase.net.manager.NetworkManagerClient;
import dev.lb.simplebase.net.packet.PacketContext;

/**
 * Sets config values for {@link NetworkManagerClient} on creation.<p>
 * Instances can be reused after they have been used to create a manager,
 * but they should be mutated on one thread only if possible.<br>
 * If the config object should be used on multiple threads (e.g. as a global config for all
 * managers, it is best practise to initialize all values once and the call {@link #lock()},
 * which prevents further modification.
 */
public class ClientConfig extends CommonConfig {
	
	public static final Object NO_CUSTOM_DATA = null;
	
	private static final ConnectionType DEFAULT_CONNECTION_TYPE = ConnectionType.DEFAULT;
	private static final Object DEFAULT_CUSTOM_DATA = NO_CUSTOM_DATA;
	
	private ConnectionType connectionType;
	private Object customData;
	
	/**
	 * Creates a new ClientConfig instance. Instance will not be locked
	 * <p>
	 * Initial values are:
	 * <table>
	 * <tr><th>Getter method name</th><th>Initial value</th></tr>
	 * <tr><td>{@link #getUseManagedThread()}</td><td>{@code true}<td></tr>
	 * <tr><td>{@link #getPacketBufferInitialSize()}</td><td>{@code 128}</td></tr>
	 * <tr><td>{@link #getConnectionCheckTimeout()}</td><td>{@code 1000}</td></tr>
	 * <tr><td>{@link #getConnectionType()}</td><td>{@link ConnectionType#DEFAULT}</td></tr>
	 * <tr><td>{@link #getCustomData()}</td><td>{@code null}</td></tr>
	 * </table>
	 */
	public ClientConfig() {
		super();
		this.connectionType = DEFAULT_CONNECTION_TYPE;
		this.customData = DEFAULT_CUSTOM_DATA;
	}
	
	public ClientConfig(CommonConfig template) {
		synchronized (template) {
			this.setUseManagedThread(template.getUseManagedThread());
			this.setConnectionCheckTimeout(template.getConnectionCheckTimeout());
			this.setPacketBufferInitialSize(template.getPacketBufferInitialSize());
			this.setGlobalConnectionCheck(template.getGlobalConnectionCheck());
			this.setCompressionSize(template.getCompressionSize());
			this.setUseEncoderThreadPool(template.getUseEncoderThreadPool());
			this.setDatagramPacketMaxSize(template.getDatagramPacketMaxSize());
			this.setUseDecoderThreadPool(template.getUseDecoderThreadPool());
		}
	}
	
	public ClientConfig(ClientConfig template) {
		synchronized (template) {
			this.setUseManagedThread(template.getUseManagedThread());
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
	 * The type of connection that will be made to the server.
	 * Not all server implementations support all connection types.
	 * @return The {@link ConnectionType} for the server connection
	 */
	public ConnectionType getConnectionType() {
		return connectionType;
	}
	
	/**
	 * The type of connection that will be made to the server.
	 * Not all server implementations support all connection types.
	 * @param The new {@link ConnectionType} for this config
	 * @throws IllegalStateException If this config object is locked ({@link #isLocked()})
	 */
	public synchronized ClientConfig setConnectionType(ConnectionType value) {
		checkLocked();
		this.connectionType = value;
		return this;
	}
	
	/**
	 * The custom object associated with the connection that the client manager will make
	 * to the server. The object will be accessible through {@link PacketContext#getCustomData()}
	 * in the packet handler.
	 * @return The custom object for the server connection
	 */
	public Object getCustomData() {
		return customData;
	}
	
	/**
	 * The custom object associated with the connection that the client manager will make
	 * to the server. The object will be accessible through {@link PacketContext#getCustomData()}
	 * in the packet handler.
	 * @param value The new custom object for the server connection
	 * @throws IllegalStateException If this config object is locked ({@link #isLocked()})
	 */
	public synchronized ClientConfig setCustomData(Object value) {
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
	public synchronized ClientConfig setUseThreadPools(boolean encoder, boolean decoder) {
		return (ClientConfig) super.setUseThreadPools(encoder, decoder);
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
	public synchronized ClientConfig setUseManagedThread(boolean value) {
		return (ClientConfig) super.setUseManagedThread(value);
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
}
