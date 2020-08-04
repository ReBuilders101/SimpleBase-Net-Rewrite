package dev.lb.simplebase.net.connection;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.SocketNetworkManagerServer;

/**
 * From Server-Side
 */
public class UdpServerSocketNetworkConnection extends ConvertingNetworkConnection {
	
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
			Object customObject, BiConsumer<InetSocketAddress, ByteBuffer> dataSender) {
		super(networkManager, remoteID, NetworkConnectionState.OPEN,
				networkManager.getConfig().getConnectionCheckTimeout(), true, customObject, false);
		if(!networkManager.supportsUdp()) throw new IllegalArgumentException("Managers for UdpServerSocketNetworkConnections must have a UdpModule");
		this.socketServer = networkManager;
		this.remoteAddress = remoteID.getFunction(NetworkIDFunction.CONNECT);
	}

	@Override
	protected void sendRawByteData(ByteBuffer buffer) {
		socketServer.sendRawUdpByteData(remoteAddress, buffer);
	}

	@Override
	protected NetworkConnectionState openConnectionImpl() {
		//this type should already be open when constructed
		STATE_LOGGER.error("Invalid state: Cannot open a server-side connection");
		return NetworkConnectionState.OPEN;
	}

	@Override
	protected NetworkConnectionState closeConnectionImpl(ConnectionCloseReason reason) {
		postEventAndRemoveConnection(reason, null);
		return NetworkConnectionState.CLOSED;
	}
	
	public void decode(ByteBuffer buffer) {
		byteToPacketConverter.acceptBytes(buffer);
	}

}
