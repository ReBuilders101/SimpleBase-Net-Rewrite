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

/**
 * A network connection that converts packets to/from bytes when sending them
 */
public abstract class ConvertingNetworkConnection extends NetworkConnection {

	protected final ByteToPacketConverter byteToPacketConverter;
	protected final PacketToByteConverter packetToByteConverter;
	protected final ConnectionAdapter connectionAdapter;
	
	protected ConvertingNetworkConnection(NetworkManagerCommon networkManager, NetworkID remoteID,
			NetworkConnectionState initialState, int checkTimeoutMS, boolean serverSide, Object customObject, boolean udpWarning) {
		super(networkManager, remoteID, initialState, checkTimeoutMS, serverSide, customObject);
		
		this.connectionAdapter = new Adapter(udpWarning);
		this.packetToByteConverter = new PacketToByteConverter(networkManager.getMappingContainer(), this::sendRawByteData,
				networkManager.getConfig().getPacketBufferInitialSize());
		this.byteToPacketConverter = new ByteToPacketConverter(connectionAdapter, networkManager.getMappingContainer(),
				networkManager.getConfig().getPacketBufferInitialSize());
	}

	protected abstract void sendRawByteData(ByteBuffer buffer);
	
	@Override
	protected boolean checkConnectionImpl(int uuid) {
		packetToByteConverter.convertAndPublish(NetworkPacketFormats.CHECK, uuid);
		return true;
	}

	@Override
	protected void sendPacketImpl(Packet packet) {
		packetToByteConverter.convertAndPublish(NetworkPacketFormats.PACKET, packet);
	}

	@Override
	public void receiveConnectionCheck(int uuid) {
		packetToByteConverter.convertAndPublish(NetworkPacketFormats.CHECKREPLY, uuid);
	}
	
	protected class Adapter implements SingleConnectionAdapter {

		private final boolean udpWarning;
		
		private Adapter(boolean udpWarning) {
			this.udpWarning = udpWarning;
		}
		
		@Override
		public void receivePacket(Packet packet) {
			ConvertingNetworkConnection.this.receivePacket(packet);
		}

		@Override
		public void receiveCheck(int uuid) {
			ConvertingNetworkConnection.this.receiveConnectionCheck(uuid);
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
		
	}
}
