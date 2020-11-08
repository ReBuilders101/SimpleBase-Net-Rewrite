package dev.lb.simplebase.net.packet.handler;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.util.Pair;

/**
 * A method that handles incoming packets from all connections of a Network Manager
 */
@FunctionalInterface
public interface PacketHandler {
	
	/**
	 * The functional method of this interface. Process a received packet
	 * @param packet The packet instance, filled with the received data
	 * @param context The {@link PacketContext} that holds information about the packet source
	 */
	public void handlePacket(Packet packet, PacketContext context);
	
	
	/**
	 * Creates a new {@link PacketHandler} that discards any received packet.<br>
	 * @return The empty packet handler
	 */
	public static PacketHandler createEmpty() {
		return new EmptyPacketHandler();
	}
	
	/**
	 * Creates a new {@link PacketHandler} that replies the packet back to the sender
	 * @return The echo packet handler
	 */
	public static PacketHandler createEcho() {
		return (p, c) -> c.replyPacket(p);
	}
	
	/**
	 * Creates a {@link PacketHandler} that can be switched out later by updating the value of the {@link AtomicReference}.
	 * @param reference The {@link AtomicReference} to the initial handler
	 * @return A {@link PacketHandler} delegating to the reference value.
	 */
	public static PacketHandler createUpdatable(AtomicReference<PacketHandler> reference) {
		return reference.get()::handlePacket;
	}
	
	public static Pair<PacketHandler, Consumer<PacketHandler>> createUpdatable(PacketHandler initial) {
		final AtomicReference<PacketHandler> ref = new AtomicReference<>(initial);
		return new Pair<>(createUpdatable(ref), ref::set);
	}
	
	/**
	 * Creates a new {@link PacketHandler} that calls both handlers in the parameters, with
	 * the goal to avoid nested handler chains by checking special implementation.
	 * @param first The existing {@link PacketHandler}
	 * @param second The {@link PacketHandler} to add to that other handler
	 * @return The composed handler that calls both of them
	 */
	public static PacketHandler combineHandlers(final PacketHandler first, final PacketHandler second) {
		//The empty handler can be discarded since it doesn't do anything
		if(first instanceof EmptyPacketHandler) {
			return second;
		} else if(second instanceof EmptyPacketHandler) {
			return first;
		//If one is already a list
		} else if(first instanceof MultiPacketHandler) {
			//If the second one is a list asd well, add the items to first
			final MultiPacketHandler first0 = (MultiPacketHandler) first;
			if(second instanceof MultiPacketHandler) {
				final MultiPacketHandler second0 = (MultiPacketHandler) second;
				second0.exclusiveThreadsafe().forEach(first0::addHandler);
			} else { //otherwise, just add the handler
				first0.addHandler(second);
			}
			return first;
		//Second, but not first   is a list
		} else if(second instanceof MultiPacketHandler) {
			final MultiPacketHandler second0 = (MultiPacketHandler) second;
			second0.addHandler(first);
			return second;
		} else { 
			//Both have to be kept, make a new multi handler
			MultiPacketHandler newHandler = new MultiPacketHandler();
			newHandler.addHandler(first);
			newHandler.addHandler(second);
			return newHandler;
		}
	}
	
}
