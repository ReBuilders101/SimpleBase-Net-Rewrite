package dev.lb.simplebase.net.connection;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.ChannelNetworkManagerServer;
import dev.lb.simplebase.net.util.Task;

public class UdpServerChannelNetworkConnection extends ExternalNetworkConnection {

	private final ChannelNetworkManagerServer server;
	private final SocketAddress remoteAddress;
	
	public UdpServerChannelNetworkConnection(ChannelNetworkManagerServer networkManager, NetworkID remoteID, Object customObject) {
		super(networkManager, remoteID, NetworkConnectionState.OPEN, networkManager.getConfig().getConnectionCheckTimeout(), true, customObject, false);
		this.server = networkManager;
		this.remoteAddress = remoteID.getFunction(NetworkIDFunction.CONNECT);
		openCompleted.release();
	}

	@Override
	protected void sendRawByteData(ByteBuffer buffer) {
		this.server.sendRawUdpByteData(remoteAddress, buffer);
	}

	@Override
	protected Task openConnectionImpl() {
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
