package dev.lb.simplebase.net.connection;

import java.nio.ByteBuffer;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.ExternalNetworkManagerServer;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.converter.ByteAccumulator;
import dev.lb.simplebase.net.packet.converter.ConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;
import dev.lb.simplebase.net.packet.converter.SingleConnectionAdapter;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;
import dev.lb.simplebase.net.util.AwaitableTask;

/**
 * A network connection that converts packets to/from bytes when sending them
 */
public abstract class ExternalNetworkConnection extends NetworkConnection {

	protected final ByteAccumulator byteToPacketConverter;
	protected final PacketToByteConverter packetToByteConverter;
	protected final ConnectionAdapter connectionAdapter;
	protected final AwaitableTask openCompleted;
	
	
	protected ExternalNetworkConnection(NetworkManagerCommon networkManager, NetworkID remoteID,
			NetworkConnectionState initialState, int checkTimeoutMS, boolean serverSide, Object customObject, boolean udpWarning) {
		super(networkManager, remoteID, initialState, checkTimeoutMS, serverSide, customObject);
		
		this.openCompleted = new AwaitableTask();
		this.connectionAdapter = new Adapter(udpWarning);
		this.packetToByteConverter = networkManager.createToByteConverter();
		this.byteToPacketConverter = new ByteAccumulator(networkManager, connectionAdapter);
	}
	
	protected abstract void sendRawByteData(ByteBuffer buffer);
	
	@Override
	protected boolean checkConnectionImpl(int uuid) {
		sendRawByteData(packetToByteConverter.convert(NetworkPacketFormats.CHECK, uuid));
		return true;
	}

	@Override
	protected void sendPacketImpl(Packet packet) {
		if(networkManager.getEncoderPool().isValidEncoderThread(networkManager)) {
			sendRawByteData(packetToByteConverter.convert(NetworkPacketFormats.PACKET, packet));
		} else {
			networkManager.getEncoderPool().encodeAndSendPacket(this, packet);
		}
	}

	@Override
	public void receiveConnectionCheck(int uuid) {
		sendRawByteData(packetToByteConverter.convert(NetworkPacketFormats.CHECKREPLY, uuid));
	}
	
	public void sendConnectionAcceptedMessage() {
		sendRawByteData(packetToByteConverter.convert(NetworkPacketFormats.CONNECTED, null));
	}
	
	public void decode(ByteBuffer data) {
		byteToPacketConverter.acceptBytes(data);
	}
	
	private void forwardServerInfoRequest() {
		if(networkManager instanceof ExternalNetworkManagerServer && remoteID.hasFunction(NetworkIDFunction.CONNECT)) {
			((ExternalNetworkManagerServer) networkManager).receiveServerInfoRequest(remoteID.getFunction(NetworkIDFunction.CONNECT));
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
