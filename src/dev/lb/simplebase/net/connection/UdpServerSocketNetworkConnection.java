package dev.lb.simplebase.net.connection;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.SocketNetworkManagerServer;
import dev.lb.simplebase.net.util.Task;

/**
 * From Server-Side
 */
public class UdpServerSocketNetworkConnection extends ExternalNetworkConnection {
	
	private final SocketAddress remoteAddress;
	private final SocketNetworkManagerServer socketServer;
	
	/**
	 * Starts as OPEN, only server-side
	 * @param networkManager
	 * @param remoteID
	 * @param checkTimeoutMS
	 * @param serverSide
	 * @param customObject
	 * @param dataSender
	 */
	public UdpServerSocketNetworkConnection(SocketNetworkManagerServer networkManager, NetworkID remoteID,
			Object customObject) {
		super(networkManager, remoteID, NetworkConnectionState.OPEN,
				networkManager.getConfig().getConnectionCheckTimeout(), true, customObject, false);
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
	
	public void decode(ByteBuffer buffer) {
		byteToPacketConverter.acceptBytes(buffer);
	}

}
