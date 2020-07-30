package dev.lb.simplebase.net.packet.handler;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.log.LogLevel;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;

/**
 * A packet handler that discards received packets.<br>
 * Will log received packets at {@link LogLevel#DEBUG}.
 * <p>
 * Classes managing handlers or handler lists are allowed to ignore or remove 
 * instances of {@link EmptyPacketHandler} from the handler chain at any time.
 */
@Internal
public final class EmptyPacketHandler implements PacketHandler {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("packet-handler");
	
	@Override
	public void handlePacket(Packet packet, PacketContext context) {
		LOGGER.debug("Packet discarded by empty handler: " + packet);
	}

}
