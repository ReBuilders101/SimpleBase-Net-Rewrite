package dev.lb.simplebase.net.packet.handler;

import dev.lb.simplebase.net.log.LogLevel;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;

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
	 * A log message at {@link LogLevel#DEBUG} will be generated for each discarded packet.
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
				second0.readOnlyThreadsafe().forEach(first0::addHandler);
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
