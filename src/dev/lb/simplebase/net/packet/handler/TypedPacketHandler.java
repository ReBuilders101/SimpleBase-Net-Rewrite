package dev.lb.simplebase.net.packet.handler;

import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;

/**
 * A functional interface for methods that accept a specific subclass of {@link Packet} only.
 * If used as a regular {@link PacketHandler}, the cast form {@code Packet} to {@code T}
 * will be unchecked.
 * @param <T> The type of packet implementation
 */
@FunctionalInterface
public interface TypedPacketHandler<T extends Packet> extends PacketHandler {

	/**
	 * The functional method.
	 * @param packet The packet instance, filled with the received data
	 * @param context The {@link PacketContext} that holds information about the packet source
	 */
	public void handleTypedPacket(T packet, PacketContext context);

	/**
	 * Implemented so this interface can work as a regular {@link PacketHandler}.
	 * Will call {@link #handleTypedPacket(Packet, PacketContext)} with an unchecked cast.
	 */
	@Override
	@Deprecated
	@SuppressWarnings("unchecked")
	public default void handlePacket(Packet packet, PacketContext context) {
		handleTypedPacket((T) packet, context);
	}
	
}
