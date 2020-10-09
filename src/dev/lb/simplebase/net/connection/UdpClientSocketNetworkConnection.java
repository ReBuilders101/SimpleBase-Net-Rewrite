package dev.lb.simplebase.net.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.AcceptorThreadDeathReason;
import dev.lb.simplebase.net.manager.NetworkManagerClient;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;
import dev.lb.simplebase.net.task.Task;
import dev.lb.simplebase.net.util.InternalAccess;

/**
 * <h2>Internal use only</h2>
 * <p>
 * This class is used internally by the API and the contained methods should not and can not be called directly.
 * </p><hr><p>
 * A {@link NetworkConnection} implementation using a {@link DatagramSocket}. Can be used only on the client side.
 * </p>
 */
public class UdpClientSocketNetworkConnection extends ExternalNetworkConnection {

	private final DatagramSocket socket;
	private final DatagramReceiverThread thread;
	private final SocketAddress remoteAddress;
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This constructor is used internally by the API and should not and can not be called directly.
	 * </p><hr><p>
	 * Create a new client-side connection implementation using a {@link DatagramSocket}.
	 * </p>
	 * @param networkManager The client manager used by this connection
	 * @param remoteID The {@link NetworkID} of the remote side of the connection
	 * @param customObject The costom data for the connection's {@link PacketContext}
	 */
	@Internal
	@SuppressWarnings("deprecation")
	public UdpClientSocketNetworkConnection(NetworkManagerClient networkManager, NetworkID remoteID, Object customObject) {
		super(networkManager, remoteID, NetworkConnectionState.INITIALIZED,
				networkManager.getConfig().getConnectionCheckTimeout(), false, customObject, false);
		InternalAccess.assertCaller(NetworkManagerClient.class, 0, "Cannot instantiate UdpClientSocketNetworkConnection directly");
		try {
			this.socket = new DatagramSocket();
		} catch (SocketException e) {
			STATE_LOGGER.fatal("Cannot bind DatagramSocket", e);
			throw new RuntimeException(e);
		}
		this.thread = new DatagramReceiverThread(socket, this::receiveRawByteData,
				this::notifyReceiverThreadDeath, networkManager.getConfig().getDatagramPacketMaxSize());
		this.remoteAddress = remoteID.getFunction(NetworkIDFunction.CONNECT);
	}

	@Override
	protected void sendRawByteData(ByteBuffer buffer) {
		final int datagramSize = networkManager.getConfig().getDatagramPacketMaxSize();
		while(buffer.hasRemaining()) {
			final int toCopy = Math.min(datagramSize, buffer.remaining());
			final byte[] array = new byte[toCopy];
			
			buffer.get(array);
			try {
				socket.send(new DatagramPacket(array, array.length, remoteAddress));
			} catch (IOException e) {
				SEND_LOGGER.warning("Cannot send raw byte message with UDP socket", e);
			}
		}
	}
	
	private void receiveRawByteData(InetSocketAddress address, ByteBuffer buffer) {
		byteAccumulator.acceptBytes(buffer);
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
		sendRawByteData(packetToByteConverter.convert(NetworkPacketFormats.LOGOUT, null));
		openCompleted.release();
		thread.interrupt(); //Will close the socket
		postEventAndRemoveConnection(reason, null);
		currentState = NetworkConnectionState.CLOSED;
		return Task.completed();
	}

}
