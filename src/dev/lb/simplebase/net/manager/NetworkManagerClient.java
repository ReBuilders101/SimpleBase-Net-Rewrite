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
import dev.lb.simplebase.net.events.PacketSendingFailedEvent;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.Logger;
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
	static final Logger LOGGER = NetworkManager.getModuleLogger("client-manager");

	private final NetworkID remoteID;
	private final NetworkConnection connection;

	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and can not be called directly.
	 * </p><hr><p>
	 * Creates a new {@link NetworkManagerClient}.
	 * </p>
	 * @param local The local {@link NetworkID} of the client
	 * @param remote The remote {@link NetworkID} of the server
	 * @param config The {@link ClientConfig} for the client
	 */
	@Internal
	public NetworkManagerClient(NetworkID local, NetworkID remote, ClientConfig config) {
		super(local, config, 1);
		this.remoteID = remote;
		this.connection = getImplementation(config.getConnectionType());
	}

	/**
	 * The {@link NetworkID} of the server that this client connects to.
	 * @return The {@link NetworkID} of the server
	 */
	public NetworkID getServerID() {
		return remoteID;
	}

	/**
	 * Opens the connection to the server.
	 * <p>
	 * Equivalent to calling {@link NetworkConnection#openConnection()} on the server connection
	 * for this client manager.
	 * </p>
	 * @return A {@link Task} that will complete once the connection is accepted and confirmed by the server
	 */
	public Task openConnectionToServer() {
		return connection.openConnection();
	}

	/**
	 * Closes the connection to the server.
	 * <p>
	 * Equivalent to calling {@link NetworkConnection#closeConnection()} on the server connection
	 * for this client manager.
	 * </p>
	 * @return A {@link Task} that will complete once the connection is closed
	 */
	public Task closeConnectionToServer() {
		return connection.closeConnection();
	}

	/**
	 * Checks the status of the connection to the server.
	 * <p>
	 * Equivalent to calling {@link NetworkConnection#checkConnection()} on the server connection
	 * for this client manager.
	 * </p>
	 * @return A {@link Task} that will complete once the connection check is completed
	 */
	public Task checkConnectionToServer() {
		return connection.checkConnection();
	}
	
	/**
	 * Sends a packet to the server.
	 * <p>
	 * Equivalent to calling {@link NetworkConnection#sendPacket(Packet)} on the server connection
	 * for this client manager.
	 * </p>
	 * @param packet The packet to send
	 * @return {@code true} if it was attempted to send the packet, {@code false} if it failed
	 * because the connection was in the wrong state. <b>If this method returns {@code false},
	 * no {@link PacketSendingFailedEvent} will be posted</b>
	 */
	public boolean sendPacketToServer(Packet packet) {
		return connection.sendPacket(packet);
	}

	/**
	 * The current {@link NetworkConnectionState} of the connection to the server.
	 * <p>
	 * Equivalent to calling {@link NetworkConnection#getCurrentState()} on the server connection
	 * for this client manager.
	 * </p>
	 * @return The current state of the server connection
	 */
	public NetworkConnectionState getServerConnectionState() {
		return connection.getCurrentState();
	}

	/**
	 * A {@link ThreadsafeAction} that can execute actions on the server connection
	 * while guaranteeing that the connection state will not change.
	 * @deprecated It is rarely necessary to exclusively lock a connection
	 * @return A {@link ThreadsafeAction} to access the server connection
	 */
	@Deprecated
	public ThreadsafeAction<NetworkConnection> getThreadsafeServerConnection() {
		return connection.threadsafe();
	}

	/**
	 * The {@link NetworkConnection} that this client uses to connect to the server
	 * @return The connection to the server
	 */
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

	@Override
	public EventAccessor<?>[] getEvents() {
		return new EventAccessor<?>[] {
			ConnectionClosed,
			PacketSendingFailed,
			PacketReceiveRejected
		};
	}

	@Override
	@SuppressWarnings("deprecation")
	public void cleanUp() {
		super.cleanUp();
		connection.threadsafe().action((con) -> { //Find a different way for this, threadsafe() is a temp fix
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
