package dev.lb.simplebase.net.manager;

import java.util.Objects;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.config.ClientConfig;
import dev.lb.simplebase.net.config.ConnectionType;
import dev.lb.simplebase.net.connection.InternalNetworkConnection;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.connection.NetworkConnectionState;
import dev.lb.simplebase.net.event.EventAccessor;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.util.ThreadsafeAction;

/**
 * The {@link NetworkManagerClient} represents the central network interface for
 * client-side code.<br>
 * The manager holds a {@link NetworkConnection} to exactly one server.
 * <p>
 * Can be created with {@link NetworkManager#createClient(NetworkID, NetworkID, ClientConfig)}.
 */
@Threadsafe
public final class NetworkManagerClient extends NetworkManagerCommon {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("client-manager");
	
	private final NetworkID remoteID;
	private final NetworkConnection connection;
	
	public NetworkManagerClient(NetworkID local, NetworkID remote, ClientConfig config) {
		super(local, config); //This will null-check most both, Below will null-check the config and type
		Objects.requireNonNull(config, "'config' parameter must not be null");
		this.connection = getImplementation(ConnectionType.resolve(config.getConnectionType(), remote));
		this.remoteID = remote;
	}

	public NetworkID getServerID() {
		return remoteID;
	}
	
	public void openConnectionToServer() {
		connection.openConnection();
	}
	
	public void closeConnectionToServer() {
		connection.closeConnection();
	}
	
	public void checkConnectionToServer() {
		connection.checkConnection();
	}
	
	public NetworkConnectionState getServerConnectionState() {
		return connection.getCurrentState();
	}
	
	public ThreadsafeAction<NetworkConnection> getThreadsafeServerConnection() {
		return connection.threadsafe();
	}
	
	public NetworkConnection getServerConnection() {
		return connection;
	}
	
	@Override
	protected PacketContext getConnectionlessPacketContext(NetworkID source) {
		if(remoteID.equals(source)) { //Even connectionless packets must come from the server
			return connection.getPacketContext();
		} else {
			return null;
		}
	}

	@Override
	public void removeConnectionSilently(NetworkConnection connection) {
		//Don't actually do anything, it is closed now anyways
		LOGGER.debug("Server connection closed (removal from client %s requested)", getLocalID());
	}

	@Override
	public ClientConfig getConfig() {
		return (ClientConfig) super.getConfig();
	}
	
	private NetworkConnection getImplementation(ConnectionType type) {
		switch (type) {
		case INTERNAL:
			return new InternalNetworkConnection(this, remoteID, getConfig().getConnectionCheckTimeout(),
					false, getConfig().getCustomData());
		default:
			throw new IllegalArgumentException("Invalid connection type: " + type);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EventAccessor<?>[] getEvents() {
		return new EventAccessor<?>[] {
			ConnectionClosed,
			PacketSendingFailed,
			PacketReceiveRejected,
			UnknownConnectionlessPacket
		};
	}

	@Override
	public void cleanUp() {
		super.cleanUp();
		connection.closeConnection();
	}

	@Override
	public void updateConnectionStatus() {
		connection.updateConnectionStatus();
	}
	
}
