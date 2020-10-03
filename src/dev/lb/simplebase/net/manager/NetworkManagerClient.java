package dev.lb.simplebase.net.manager;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.config.ClientConfig;
import dev.lb.simplebase.net.config.ConnectionType;
import dev.lb.simplebase.net.connection.InternalNetworkConnection;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.connection.NetworkConnectionState;
import dev.lb.simplebase.net.connection.TcpSocketNetworkConnection;
import dev.lb.simplebase.net.connection.UdpClientSocketNetworkConnection;
import dev.lb.simplebase.net.event.EventAccessor;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.task.Task;
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

	@Internal
	public NetworkManagerClient(NetworkID local, NetworkID remote, ClientConfig config, int depth) {
		super(local, config, depth + 1);
		this.remoteID = remote;
		this.connection = getImplementation(config.getConnectionType());
	}

	public NetworkID getServerID() {
		return remoteID;
	}

	public Task openConnectionToServer() {
		return connection.openConnection();
	}

	public Task closeConnectionToServer() {
		return connection.closeConnection();
	}

	public Task checkConnectionToServer() {
		return connection.checkConnection();
	}
	
	public boolean sendPacketToServer(Packet packet) {
		return connection.sendPacket(packet);
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
	public void removeConnectionSilently(NetworkConnection connection) {
		//Don't actually do anything, it is closed now anyways
		LOGGER.debug("Server connection closed (removal from client '%s' requested)", getLocalID().getDescription());
	}

	@Override
	public ClientConfig getConfig() {
		return (ClientConfig) super.getConfig();
	}

	private NetworkConnection getImplementation(ConnectionType type) {
		switch (type) {
		case INTERNAL:
			return new InternalNetworkConnection(this, remoteID, getConfig().getCustomData());
		case TCP:
			return new TcpSocketNetworkConnection(this, remoteID, getConfig().getCustomData());
		case UDP:
			return new UdpClientSocketNetworkConnection(this, remoteID, getConfig().getCustomData());
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
			PacketReceiveRejected
		};
	}

	@Override
	public void cleanUp() {
		super.cleanUp();
		connection.threadsafe().action((con) -> {
			if(connection.getCurrentState() != NetworkConnectionState.CLOSED) {
				connection.closeConnection();
			}
		});
	}

	@Override
	public void updateConnectionStatus() {
		connection.updateConnectionStatus();
	}

	@Override
	@Deprecated
	public boolean sendPacketTo(NetworkID remote, Packet packet) {
		if(remote == remoteID) {
			return sendPacketToServer(packet);
		} else {
			return false;
		}
	}

}
