package dev.lb.simplebase.net.events;

import dev.lb.simplebase.net.NetworkManagerCommon;
import dev.lb.simplebase.net.annotation.Immutable;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.event.Event;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.LogLevel;
import dev.lb.simplebase.net.packet.Packet;

/**
 * Created when a packet could not be handled by the normal handlers.
 * <p>
 * <b>Cancelling</b> will cause the message about a rejected packet to be logged at {@link LogLevel#DEBUG}
 * instead of {@link LogLevel#WARNING}.
 * Cancelling this event indicates that the application is aware of the problem and can continue normally.
 * @see NetworkManagerCommon#PacketReceiveRejected
 */
@Immutable
public class PacketRejectedEvent extends Event {

	private final NetworkID source;
	private final Class<? extends Packet> packetType;
	
	//Deliberately don't include the packet data, the event handler should not receive the packet
	/**
	 * Params can't be null!!
	 */
	@Internal
	public PacketRejectedEvent(NetworkID source, Class<? extends Packet> packetType) {
		super(true, false);
		this.source = source;
		this.packetType = packetType;
	}

	/**
	 * The {@link NetworkID} of the remote source that sent the rejected packet
	 * @return The NetworkID of the packet source
	 */
	public NetworkID getPacketSourceID() {
		return source;
	}
	
	/**
	 * The {@link Class} of the packet that was rejected
	 * @return The rejected packet's type
	 */
	public Class<? extends Packet> getPacketType() {
		return packetType;
	}
	
}
