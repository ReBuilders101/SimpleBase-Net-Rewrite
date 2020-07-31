package dev.lb.simplebase.net.packet.converter;

import dev.lb.simplebase.net.connection.NetworkConnection;

/**
 * Adapter that represents a single {@link NetworkConnection}.<br>
 * Produces a warning when {@link #receiveUdpLogin()} is called.
 */
public interface SingleConnectionAdapter extends ConnectionAdapter {
	
	@Override
	public default void receiveUdpLogin() {
		PacketToByteConverter.CONNECTION_LOGGER.warning("Unexpected packet: Received UDP-Login for an existing connection implementation");
	}
	
}
