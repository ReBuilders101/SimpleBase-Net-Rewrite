package dev.lb.simplebase.net.events;

import java.util.concurrent.RejectedExecutionException;

import dev.lb.simplebase.net.event.Event;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.packet.Packet;

public class PacketSendingFailedEvent extends Event {

	private final NetworkID destination;
	private final Class<? extends Packet> packetType;
	private final RejectedExecutionException exception;
	
	//Cancel to prevent warning
	public PacketSendingFailedEvent(RejectedExecutionException e) {
		super(true, false);
		this.destination = null;
		this.packetType = null;
		this.exception = e;
	}
	
	public PacketSendingFailedEvent(NetworkID destination, Packet packet) {
		super(true, false);
		this.destination = destination;
		this.packetType = packet.getClass();
		this.exception = null;
	}
	
	/**
	 * The {@link NetworkID} of the remote source that sent the rejected packet
	 * @return The NetworkID of the packet source
	 */
	public NetworkID getPacketDestinationID() {
		return destination;
	}
	
	/**
	 * The {@link Class} of the packet that was rejected
	 * @return The rejected packet's type
	 */
	public Class<? extends Packet> getPacketType() {
		return packetType;
	}
	
	public RejectedExecutionException getException() {
		return exception;
	}
	
	public boolean hasDetails() {
		return exception == null;
	}
}
