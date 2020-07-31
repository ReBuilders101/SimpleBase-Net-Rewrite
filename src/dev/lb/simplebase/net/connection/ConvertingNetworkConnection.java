package dev.lb.simplebase.net.connection;

import dev.lb.simplebase.net.NetworkConnectionState;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.converter.ByteToPacketConverter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;
import dev.lb.simplebase.net.packet.converter.TcpConnectionAdapter;

/**
 * A network connection that converts packets to/from bytes when sending them
 */
public abstract class ConvertingNetworkConnection extends NetworkConnection {

	protected final ByteToPacketConverter byteToPacketConverter;
	protected final PacketToByteConverter packetToByteConverter;
	protected final TcpConnectionAdapter connectionAdapter;
	
	protected ConvertingNetworkConnection(NetworkManagerCommon networkManager, NetworkID remoteID,
			NetworkConnectionState initialState, int checkTimeoutMS, boolean serverSide, Object customObject) {
		super(networkManager, remoteID, initialState, checkTimeoutMS, serverSide, customObject);
		
		this.connectionAdapter = new Adapter();
		this.packetToByteConverter = new PacketToByteConverter(networkManager.getMappingContainer(), this::sendRawByteData);
		this.byteToPacketConverter = new ByteToPacketConverter(connectionAdapter, networkManager.getMappingContainer());
	}

	
	private class Adapter implements TcpConnectionAdapter {

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
		
	}
}
