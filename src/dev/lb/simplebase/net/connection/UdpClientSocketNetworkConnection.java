package dev.lb.simplebase.net.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.AcceptorThreadDeathReason;
import dev.lb.simplebase.net.manager.NetworkManagerClient;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;
import dev.lb.simplebase.net.util.Task;

public class UdpClientSocketNetworkConnection extends ExternalNetworkConnection {

	private final DatagramSocket socket;
	private final DatagramSocketReceiverThread thread;
	private final SocketAddress remoteAddress;
	
	public UdpClientSocketNetworkConnection(NetworkManagerClient networkManager, NetworkID remoteID, Object customObject) {
		super(networkManager, remoteID, NetworkConnectionState.INITIALIZED,
				networkManager.getConfig().getConnectionCheckTimeout(), false, customObject, false);
		try {
			this.socket = new DatagramSocket();
		} catch (SocketException e) {
			STATE_LOGGER.fatal("Cannot bind DatagramSocket", e);
			throw new RuntimeException(e);
		}
		this.thread = new DatagramSocketReceiverThread(socket, this::receiveRawByteData,
				this::notifyReceiverThreadDeath, networkManager.getConfig().getPacketBufferInitialSize());
		this.remoteAddress = remoteID.getFunction(NetworkIDFunction.CONNECT);
	}

	@Override
	protected void sendRawByteData(ByteBuffer buffer) {
		final byte[] array = new byte[buffer.remaining()];
		buffer.get(array);
		try {
			socket.send(new DatagramPacket(array, array.length, remoteAddress));
		} catch (IOException e) {
			SEND_LOGGER.warning("Cannot send raw byte message with UDP socket", e);
		}
	}
	
	private void receiveRawByteData(InetSocketAddress address, ByteBuffer buffer) {
		byteToPacketConverter.acceptBytes(buffer);
	}
	
	private void notifyReceiverThreadDeath(AcceptorThreadDeathReason reason) {
		//Empty on purpose, for now
	}

	@Override
	protected Task openConnectionImpl() {
		try {
			socket.connect(remoteAddress);
		} catch (SocketException e) {
			STATE_LOGGER.error("Error while connecting UDP socket", e);
			currentState = NetworkConnectionState.CLOSED;
		}
		thread.start();
		sendRawByteData(packetToByteConverter.convert(NetworkPacketFormats.LOGIN, null));
//		currentState = NetworkConnectionState.OPEN; //When ACK comes
		return openCompleted;
	}

	@Override
	protected Task closeConnectionImpl(ConnectionCloseReason reason) {
		openCompleted.release();
		thread.interrupt(); //Will close the socket
		postEventAndRemoveConnection(reason, null);
		currentState = NetworkConnectionState.CLOSED;
		return Task.completed();
	}

}
