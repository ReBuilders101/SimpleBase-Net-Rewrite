package dev.lb.simplebase.net.events;

import java.util.concurrent.RejectedExecutionException;

import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.event.Event;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.LogLevel;
import dev.lb.simplebase.net.packet.Packet;

/**
 * Called when a packet could not be successfully sent through a {@link NetworkConnection}.
 * <p>
 * <b>Cancelling</b> will cause the message about a rejected packet to be logged at {@link LogLevel#DEBUG}
 * instead of {@link LogLevel#WARNING}.
 * Cancelling this event indicates that the application is aware of the problem and can continue normally.
 * </p>
 */
public class PacketSendingFailedEvent extends Event {

	private final NetworkID destination;
	private final Class<? extends Packet> packetType;
	private final RejectedExecutionException exception;
	
	/**
	 * Create a new instance of this event based on a {@link RejectedExecutionException} that happened
	 * when trying to push the received packet data to a different thread (pool) for the next processing stage
	 * @param reject The {@link RejectedExecutionException} that caused this event
	 */
	public PacketSendingFailedEvent(RejectedExecutionException reject) {
		super(true, false);
		this.destination = null;
		this.packetType = null;
		this.exception = reject;
	}
	
	/**
	 * Creates an instance of this event for a sent packet and packet destination.
	 * @param destination The {@link NetworkID} that  the packet was going to be sent to
	 * @param packet The {@link Packet} that was supposed to be sent
	 */
	public PacketSendingFailedEvent(NetworkID destination, Packet packet) {
		super(true, false);
		this.destination = destination;
		this.packetType = packet.getClass();
		this.exception = null;
	}
	
	/**
	 * The {@link NetworkID} of the remote target for the packet,
	 * or {@code null} if this event was fired because of a {@link RejectedExecutionException}.
	 * @return The NetworkID of the packet destination
	 */
	public NetworkID getPacketDestinationID() {
		return destination;
	}
	
	/**
	 * The {@link Class} of the packet that was not sent,
	 * or {@code null} if this event was fired because of a {@link RejectedExecutionException}.
	 * @return The packet's type
	 */
	public Class<? extends Packet> getPacketType() {
		return packetType;
	}
	
	/**
	 * The {@link RejectedExecutionException} that caused packet sending to fail,
	 * or {@code null} if this event was not fired because of a {@link RejectedExecutionException}.
	 * @return The exception that caused this event
	 */
	public RejectedExecutionException getException() {
		return exception;
	}
	
	/**
	 * If {@code true}, the {@link #getPacketDestinationID()} and {@link #getPacketType()} methods will return meaningful values,
	 * while {@link #getException()} is {@code null}. If {@code false}, only {@link #getException()} will return a value
	 * and the other two methods will return {@code null}.
	 * @return Whether packet source and type are present
	 */
	public boolean hasDetails() {
		return exception == null;
	}
}
