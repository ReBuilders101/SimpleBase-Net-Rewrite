package dev.lb.simplebase.net.events;

import java.util.concurrent.RejectedExecutionException;

import dev.lb.simplebase.net.annotation.Immutable;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.event.Event;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.LogLevel;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;

/**
 * Created when a packet could not be handled by the normal handlers.
 * <p>
 * <b>Cancelling</b> will cause the message about a rejected packet to be logged at {@link LogLevel#DEBUG}
 * instead of {@link LogLevel#WARNING}.
 * Cancelling this event indicates that the application is aware of the problem and can continue normally.
 * </p>
 * @see NetworkManagerCommon#PacketReceiveRejected
 */
@Immutable
public class PacketReceiveRejectedEvent extends Event {

	private final NetworkID source;
	private final Class<? extends Packet> packetType;
	private final RejectedExecutionException exception;
	
	//Deliberately don't include the packet data, the event handler should not receive the packet
	/**
	 * Creates an instance of this event for a received packet source and type.
	 * <p>
	 * The packet itself is never stored with this event, as the handling might be incomplete and this method
	 * is not meant for processing a packet, only to be notified of an error.
	 * </p>
	 * @param source The {@link NetworkID} that sent the packet
	 * @param packetType The {@link Class} of the received packet
	 */
	@Internal
	public PacketReceiveRejectedEvent(NetworkID source, Class<? extends Packet> packetType) {
		super(true, false);
		this.source = source;
		this.packetType = packetType;
		this.exception = null;
	}
	
	/**
	 * Create a new instance of this event based on a {@link RejectedExecutionException} that happened
	 * when trying to push the received packet data to a different thread (pool) for the next processing stage
	 * @param reject The {@link RejectedExecutionException} that caused this event
	 */
	@Internal
	public PacketReceiveRejectedEvent(RejectedExecutionException reject) {
		super(true, false);
		this.source = null;
		this.packetType = null;
		this.exception = reject;
	}

	/**
	 * The {@link NetworkID} of the remote source that sent the rejected packet,
	 * or {@code null} if this event was fired because of a {@link RejectedExecutionException}.
	 * @return The NetworkID of the packet source
	 */
	public NetworkID getPacketSourceID() {
		return source;
	}
	
	/**
	 * The {@link Class} of the packet that was rejected,
	 * or {@code null} if this event was fired because of a {@link RejectedExecutionException}.
	 * @return The rejected packet's type
	 */
	public Class<? extends Packet> getPacketType() {
		return packetType;
	}
	
	/**
	 * The {@link RejectedExecutionException} that caused the packet handling to fail,
	 * or {@code null} if this event was not fired because of a {@link RejectedExecutionException}.
	 * @return The exception that caused this event
	 */
	public RejectedExecutionException getException() {
		return exception;
	}
	
	/**
	 * If {@code true}, the {@link #getPacketSourceID()} and {@link #getPacketType()} methods will return meaningful values,
	 * while {@link #getException()} is {@code null}. If {@code false}, only {@link #getException()} will return a value
	 * and the other two methods will return {@code null}.
	 * @return Whether packet source and type are present
	 */
	public boolean hasDetails() {
		return exception == null;
	}
	
}
