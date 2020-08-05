package dev.lb.simplebase.net.packet.converter;

import dev.lb.simplebase.net.packet.Packet;

/**
 * Only accepts SERVERINFOAN
 */
public interface AnonymousClientConnectionAdapter extends ConnectionAdapter {

	@Override
	public default void receiveUdpLogin() {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received UDP-Login for an anonymous client connection implementation");
	}

	@Override
	public default void receiveUdpLogout() {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received UDP-Logout for an anonymous client connection implementation");
	}

	@Override
	public default void receivePacket(Packet packet) {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received regular Packet for an anonymous client connection implementation");
	}

	@Override
	public default void receiveCheck(int uuid) {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received Check-Request for an anonymous client connection implementation");
	}

	@Override
	public default void receiveCheckReply(int uuid) {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received Check-Reply for an anonymous client connection implementation");
	}

	@Override
	public default void receiveServerInfoRequest() {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received Server-Info-Request for an anonymous client connection implementation");
	}

	@Override
	public default void receiveConnectionAccepted() {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received Connection-Accepted for an anonymous client connection implementation");
	}

}
