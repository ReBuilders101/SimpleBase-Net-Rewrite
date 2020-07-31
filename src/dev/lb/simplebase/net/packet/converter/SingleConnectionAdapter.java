package dev.lb.simplebase.net.packet.converter;

public interface SingleConnectionAdapter extends ConnectionAdapter {
	
	@Override
	public default void receiveUdpLogin() {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received UDP-Login for an existing connection implementation");
	}
	
}
