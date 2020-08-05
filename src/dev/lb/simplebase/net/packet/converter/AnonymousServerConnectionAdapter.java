package dev.lb.simplebase.net.packet.converter;

import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.packet.Packet;

/**
 * A {@link ConnectionAdapter} that is not owned by a {@link NetworkConnection},
 * for example because it receives UDP_LOGINS before connection creation or
 * handles SERVERINFORQs.<br>>Exists on the server side.
 */
public interface AnonymousServerConnectionAdapter extends ConnectionAdapter {
	
	@Override
	public default void receiveUdpLogout() {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received UDP-Logut for an anonymous connection implementation");
	}

	@Override
	public default void receivePacket(Packet packet) {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received PACKET for an anonymous connection implementation");
	}

	@Override
	public default void receiveCheck(int uuid) {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received Check-Request for an anonymous connection implementation");
	}

	@Override
	public default void receiveCheckReply(int uuid) {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received Check-Reply for an anonymous connection implementation");
	}

	@Override
	public default void receiveServerInfoPacket(Packet packet) {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received Server-Info-Answer for an anonymous connection implementation");
	}

	@Override
	public default void receiveConnectionAccepted() {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received Connection-Accepted for an anonymous connection implementation");
	}

}
