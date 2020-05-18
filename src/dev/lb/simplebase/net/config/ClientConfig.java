package dev.lb.simplebase.net.config;

import dev.lb.simplebase.net.NetworkManagerClient;
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
	
	private ConnectionType connectionType;
	private Object customData;
	
	/**
	 * Creates a new ClientConfig instance. Instance will not be locked
	 * <p>
	 * Initial values are:
	 * <table>
	 * <tr><th>Getter method name</th><th>Initial value</th></tr>
	 * <tr><td>{@link #getUseManagedThread()}</td><td>{@code true}<td></tr>
	 * <tr><td>{@link #getEncodeBufferInitialSize()}</td><td>{@code 128}</td></tr>
	 * <tr><td>{@link #getConnectionCheckTimeout()}</td><td>{@code 1000}</td></tr>
	 * <tr><td>{@link #getConnectionType()}</td><td>{@link ConnectionType#DEFAULT}</td></tr>
	 * <tr><td>{@link #getCustomData()}</td><td>{@code null}</td></tr>
	 * </table>
	 */
	public ClientConfig() {
		super();
		this.connectionType = ConnectionType.DEFAULT;
		this.customData = null;
	}
	
	/**
	 * The type of connection that will be made to the server.
	 * Not all server implementations support all connection types.
	 * @return The {@link ConnectionType} for the server connection
	 */
	public synchronized ConnectionType getConnectionType() {
		return connectionType;
	}
	
	/**
	 * The type of connection that will be made to the server.
	 * Not all server implementations support all connection types.
	 * @param The new {@link ConnectionType} for this config
	 * @throws IllegalStateException If this config object is locked ({@link #isLocked()})
	 */
	public synchronized void setConnectionType(ConnectionType value) {
		checkLocked();
		this.connectionType = value;
	}
	
	/**
	 * The custom object associated with the connection that the client manager will make
	 * to the server. The object will be accessible through {@link PacketContext#getCustomData()}
	 * in the packet handler.
	 * @return The custom object for the server connection
	 */
	public synchronized Object getCustomData() {
		return customData;
	}
	
	/**
	 * The custom object associated with the connection that the client manager will make
	 * to the server. The object will be accessible through {@link PacketContext#getCustomData()}
	 * in the packet handler.
	 * @param value The new custom object for the server connection
	 * @throws IllegalStateException If this config object is locked ({@link #isLocked()})
	 */
	public synchronized void setCustomData(Object value) {
		checkLocked();
		this.customData = value;
	}
}
