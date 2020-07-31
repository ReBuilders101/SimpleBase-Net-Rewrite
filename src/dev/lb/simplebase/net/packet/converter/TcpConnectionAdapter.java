package dev.lb.simplebase.net.packet.converter;

public interface TcpConnectionAdapter extends ConnectionAdapter {
	
	@Override
	public default void receiveUdpLogin() {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received UDP-Login for a TCP connection implementation");
	}

	@Override
	public default void receiveUdpLogout() {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received UDP-Logout for a TCP connection implementation");
	}
	
}
