package dev.lb.simplebase.net.connection;

import java.nio.ByteBuffer;

import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.converter.ByteToPacketConverter;
import dev.lb.simplebase.net.packet.converter.ConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;
import dev.lb.simplebase.net.packet.converter.SingleConnectionAdapter;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;
import dev.lb.simplebase.net.util.AwaitableTask;

/**
 * A network connection that converts packets to/from bytes when sending them
 */
public abstract class ExternalNetworkConnection extends NetworkConnection {

	protected final ByteToPacketConverter byteToPacketConverter;
	protected final PacketToByteConverter packetToByteConverter;
	protected final ConnectionAdapter connectionAdapter;
	protected final AwaitableTask openCompleted;
	
	protected ExternalNetworkConnection(NetworkManagerCommon networkManager, NetworkID remoteID,
			NetworkConnectionState initialState, int checkTimeoutMS, boolean serverSide, Object customObject, boolean udpWarning) {
		super(networkManager, remoteID, initialState, checkTimeoutMS, serverSide, customObject);
		
		this.openCompleted = new AwaitableTask();
		this.connectionAdapter = new Adapter(udpWarning);
		this.packetToByteConverter = new PacketToByteConverter(networkManager.getMappingContainer(),
				networkManager.getConfig().getPacketBufferInitialSize());
		this.byteToPacketConverter = new ByteToPacketConverter(connectionAdapter, networkManager.getMappingContainer(),
				networkManager.getConfig().getPacketBufferInitialSize());
	}

	protected abstract void sendRawByteData(ByteBuffer buffer);
	
	@Override
	protected boolean checkConnectionImpl(int uuid) {
		sendRawByteData(packetToByteConverter.convert(NetworkPacketFormats.CHECK, uuid));
		return true;
	}

	@Override
	protected void sendPacketImpl(Packet packet) {
		sendRawByteData(packetToByteConverter.convert(NetworkPacketFormats.PACKET, packet));
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
		
	}
}
