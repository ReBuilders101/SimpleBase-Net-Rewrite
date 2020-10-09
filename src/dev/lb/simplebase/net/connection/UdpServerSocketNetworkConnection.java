package dev.lb.simplebase.net.connection;

import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.ExternalNetworkManagerServer;
import dev.lb.simplebase.net.manager.SocketNetworkManagerServer;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.task.Task;
import dev.lb.simplebase.net.util.InternalAccess;

/**
 * <h2>Internal use only</h2>
 * <p>
 * This class is used internally by the API and the contained methods should not and can not be called directly.
 * </p><hr><p>
 * A {@link NetworkConnection} implementation using a {@link DatagramSocket}. Can be used only on the server side.
 * </p>
 */
@Internal
public class UdpServerSocketNetworkConnection extends ExternalNetworkConnection {
	
	private final SocketAddress remoteAddress;
	private final SocketNetworkManagerServer socketServer;
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This constructor is used internally by the API and should not and can not be called directly.
	 * </p><hr><p>
	 * Create a new server-side connection implementation using a common {@link DatagramSocket}
	 * that is stored in the associated server object.
	 * </p>
	 * @param networkManager The server manager with common socket used by this connection
	 * @param remoteID The {@link NetworkID} of the remote side of the connection
	 * @param customObject The costom data for the connection's {@link PacketContext}
	 */
	@Internal
	public UdpServerSocketNetworkConnection(SocketNetworkManagerServer networkManager, NetworkID remoteID,
			Object customObject) {
		super(networkManager, remoteID, NetworkConnectionState.OPEN,
				networkManager.getConfig().getConnectionCheckTimeout(), true, customObject, false);
		InternalAccess.assertCaller(ExternalNetworkManagerServer.class, 1, "Cannot instantiate UdpServerSocketNetworkConnection directly");
		if(!networkManager.supportsUdp()) throw new IllegalArgumentException("Managers for UdpServerSocketNetworkConnections must have a UdpModule");
		this.socketServer = networkManager;
		this.remoteAddress = remoteID.getFunction(NetworkIDFunction.CONNECT);
		openCompleted.release(); //Already open
	}

	@Override
	protected void sendRawByteData(ByteBuffer buffer) {
		socketServer.sendRawUdpByteData(remoteAddress, buffer);
	}

	@Override
	protected Task openConnectionImpl() {
		//this type should already be open when constructed
		STATE_LOGGER.error("Invalid state: Cannot open a server-side connection");
		currentState = NetworkConnectionState.OPEN;
		return openCompleted;
	}

	@Override
	protected Task closeConnectionImpl(ConnectionCloseReason reason) {
		postEventAndRemoveConnection(reason, null);
		currentState = NetworkConnectionState.CLOSED;
		return Task.completed();
	}

}
