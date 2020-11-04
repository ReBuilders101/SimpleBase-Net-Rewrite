package dev.lb.simplebase.net.connection;

import java.nio.ByteBuffer;
import java.util.function.Predicate;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.events.PacketSendingFailedEvent;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFeature;
import dev.lb.simplebase.net.manager.ExternalNetworkManagerServer;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.converter.ByteAccumulator;
import dev.lb.simplebase.net.packet.converter.ConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;
import dev.lb.simplebase.net.packet.converter.SingleConnectionAdapter;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;
import dev.lb.simplebase.net.task.AwaitableTask;

/**
 * An abstract baseclass for any {@link NetworkConnection} implementation that sends networking data
 * through a byte stream or channel.
 * <p>
 * Contains common features for packet<->byte encoding/decoding.
 * </p>
 */
@Internal
public abstract class ExternalNetworkConnection extends NetworkConnection {

	protected final ByteAccumulator byteAccumulator;
	protected final PacketToByteConverter packetToByteConverter;
	protected final ConnectionAdapter connectionAdapter;
	protected final AwaitableTask openCompleted;
	protected final Predicate<Packet> sendingFailed;
	
	protected ExternalNetworkConnection(NetworkManagerCommon networkManager, NetworkID remoteID,
			NetworkConnectionState initialState, int checkTimeoutMS, boolean serverSide, Object customObject, boolean udpWarning) {
		super(networkManager, remoteID, initialState, checkTimeoutMS, serverSide, customObject);
		
		this.sendingFailed = networkManager.getEventDispatcher().p1Dispatcher(networkManager.PacketSendingFailed,
				(p) -> new PacketSendingFailedEvent(remoteID, p));
		this.openCompleted = new AwaitableTask();
		this.connectionAdapter = new Adapter(udpWarning);
		this.packetToByteConverter = networkManager.createToByteConverter();
		this.byteAccumulator = new ByteAccumulator(networkManager, connectionAdapter);
	}
	
	protected abstract void sendRawByteData(ByteBuffer buffer);
	
	@Override
	protected boolean checkConnectionImpl(int uuid) {
		sendRawByteData(packetToByteConverter.convert(NetworkPacketFormats.CHECK, uuid));
		return true;
	}

	@Override
	protected void sendPacketImpl(Packet packet) {
		if(networkManager.getEncoderPool().isValidCoderThread()) {
			final ByteBuffer encodedData = packetToByteConverter.convert(NetworkPacketFormats.PACKET, packet);
			if(encodedData != null) {
				sendRawByteData(encodedData);
			} else {
				sendingFailed.test(packet);
			}
		} else {
			networkManager.getEncoderPool().encodeAndSendPacket(this, packet);
		}
	}

	@Override
	public void receiveConnectionCheck(int uuid) {
		sendRawByteData(packetToByteConverter.convert(NetworkPacketFormats.CHECKREPLY, uuid));
	}
	
	/**
	 * <b>Called only by the server manager</b> - Don't call manually
	 * <p>
	 * Notifies the remote side of the connection that a server has successfully accepted the connection
	 * by sending a {@link NetworkPacketFormats#CONNECTED} message.
	 * </p>
	 */
	public void sendConnectionAcceptedMessage() {
		sendRawByteData(packetToByteConverter.convert(NetworkPacketFormats.CONNECTED, null));
	}
	
	/**
	 * Called by the receiver thread when data was received on the connection.
	 * <p>
	 * If called manually, it will simulate received data. If incomplete or invalid data
	 * is supplied, packet decoding may stop decoding further packets.
	 * </p>
	 * @param data The received bytes
	 */
	public void decode(ByteBuffer data) {
		byteAccumulator.acceptBytes(data);
	}
	
	private void forwardServerInfoRequest() {
		if(networkManager instanceof ExternalNetworkManagerServer && remoteID.hasFeature(NetworkIDFeature.CONNECT)) {
			((ExternalNetworkManagerServer) networkManager).receiveServerInfoRequest(remoteID.getFeature(NetworkIDFeature.CONNECT));
		} else {
			RECEIVE_LOGGER.warning("Unexpected packet: Received ServerInfoRequest for an invalid manager type");
		}
	}
	
	protected class Adapter implements SingleConnectionAdapter {

		private final boolean udpWarning;
		
		private Adapter(boolean udpWarning) {
			this.udpWarning = udpWarning;
		}
		
		@Override
		public void receivePacket(Packet packet) {
			ExternalNetworkConnection.this.receivePacket(packet);
		}

		@Override
		public void receiveCheck(int uuid) {
			ExternalNetworkConnection.this.receiveConnectionCheck(uuid);
		}

		@Override
		public void receiveCheckReply(int uuid) {
			pingTracker.confirmPing(uuid);
		}

		@Override
		public void receiveUdpLogout() {
			if(udpWarning) RECEIVE_LOGGER.warning("Unexpected packet: Received UDP-Login for an existing connection implementation");
			closeConnection(ConnectionCloseReason.REMOTE);
		}

		@Override
		public void receiveConnectionAccepted() {
			synchronized (lockCurrentState) {
				currentState = NetworkConnectionState.OPEN;
				ExternalNetworkConnection.this.openCompleted.release();
			}
		}

		@Override
		public void receiveServerInfoRequest() {
			ExternalNetworkConnection.this.forwardServerInfoRequest();
		}
		
	}
}
