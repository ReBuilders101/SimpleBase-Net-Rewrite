package dev.lb.simplebase.net;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;

/**
 * Does absolutely nothing with received packets (except a debug log)
 */
@Internal
final class EmptyPacketHandler implements PacketHandler {

	@Override
	public void handlePacket(Packet packet, PacketContext context) {
		NetworkManager.NET_LOG.debug("Packet discarded by empty handler: " + packet);
	}

}
