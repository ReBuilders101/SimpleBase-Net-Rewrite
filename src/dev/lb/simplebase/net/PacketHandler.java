package dev.lb.simplebase.net;

import dev.lb.simplebase.net.log.LogLevel;
import dev.lb.simplebase.net.packet.Packet;

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
	 * Creates a new {@link PacketHandler} that calls both handlers in the parameters, with
	 * the goal to avoid nested handler chains by checking special implementation.
	 * @param existing The existing {@link PacketHandler}
	 * @param toAdd The {@link PacketHandler} to add to that other handler
	 * @return The composed handler that calls both of them
	 */
	public static PacketHandler addHandler(PacketHandler existing, PacketHandler toAdd) {
		if(existing instanceof EmptyPacketHandler) {
			//The empty handler can be discarded since it doesn't do anything
			return toAdd;
		} else if(existing instanceof MultiPacketHandler) {
			//Add it to the list
			((MultiPacketHandler) existing).addHandler(toAdd);
			return existing;
		} else { 
			//Both have to be kept, make a new multi handler
			MultiPacketHandler newHandler = new MultiPacketHandler();
			newHandler.addHandler(existing);
			newHandler.addHandler(toAdd);
			return newHandler;
		}
	}
	
}
