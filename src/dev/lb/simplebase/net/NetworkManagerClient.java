package dev.lb.simplebase.net;

import java.util.Objects;

import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.config.ClientConfig;
import dev.lb.simplebase.net.config.ConnectionType;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.event.EventAccessor;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.PacketContext;

/**
 * The {@link NetworkManagerClient} represents the central network interface for
 * client-side code.<br>
 * The manager holds a {@link NetworkConnection} to exactly one server.
 * <p>
 * Can be created with {@link NetworkManager#createClient(NetworkID, NetworkID, ClientConfig)}.
 */
@Threadsafe
public final class NetworkManagerClient extends NetworkManagerCommon {

	private final NetworkID remoteID;
	private final NetworkConnection connection;
	
	protected NetworkManagerClient(NetworkID local, NetworkID remote, ClientConfig config) {
		super(local, config); //This will null-check most both, Below will null-check the config and type
		Objects.requireNonNull(config, "'config' parameter must not be null");
		this.connection = getImplementation(ConnectionType.resolve(config.getConnectionType(), remote));
		this.remoteID = remote;
	}

	public NetworkID getServerID() {
		return remoteID;
	}
	
	public boolean openConnectionToServer() {
		return connection.openConnection();
	}
	
	public boolean closeConnectionToServer() {
		return connection.closeConnection();
	}
	
	public boolean checkConnectionToServer() {
		return connection.checkConnection();
	}
	
	public NetworkConnectionState getServerConnectionState() {
		return connection.getCurrentState();
	}
	
	public NetworkConnection getServerConnection() {
		return connection;
	}
	
	@Override
	protected PacketContext getConnectionlessPacketContext(NetworkID source) {
		if(remoteID.equals(source)) { //Even connectionless packets must come from the server
			return connection.getContext();
		} else {
			return null;
		}
	}

	@Override
	protected void removeConnectionSilently(NetworkConnection connection) {
		//Client does not remove its connection
		NetworkManager.NET_LOG.debug("Client Manager: Remove connection called");
	}

	@Override
	public ClientConfig getConfig() {
		return (ClientConfig) super.getConfig();
	}
	
	private NetworkConnection getImplementation(ConnectionType type) {
		switch (type) {
		case INTERNAL:
			return new LocalPeerNetworkConnection(getLocalID(), remoteID, this,
					getConfig().getConnectionCheckTimeout(), false, getConfig().getCustomData());
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
			ConnectionCheckSuccess,
			PacketSendingFailed,
			PacketReceiveRejected,
			UnknownConnectionlessPacket
		};
	}

	@Override
	protected void onCheckConnectionStatus() {
		connection.updateConnectionStatus();
	}

	@Override
	public void cleanUp() {
		super.cleanUp();
		connection.closeConnection();
	}
	
}
