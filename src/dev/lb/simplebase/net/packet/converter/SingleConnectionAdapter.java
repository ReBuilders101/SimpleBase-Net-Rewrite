package dev.lb.simplebase.net.packet.converter;

import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.packet.Packet;

/**
 * Adapter that represents a single {@link NetworkConnection}.<br>
 * Produces a warning when {@link #receiveUdpLogin()} is called.
 */
public interface SingleConnectionAdapter extends ConnectionAdapter {
	
	@Override
	public default void receiveUdpLogin() {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received UDP-Login for an existing connection implementation");
	}

//	@Override
//	public void receiveServerInfoRequest() {
//		//IPs that already hold a connection can still ask for the packet
//		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received Server-Info-Request for an existing connection implementation");
//	}

	@Override
	public default void receiveServerInfoPacket(Packet packet) {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received Server-Info-Packet for an existing connection implementation");
	}
	
}
