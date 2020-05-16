package dev.lb.simplebase.net;

import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.packet.Packet;

/**
 * Provides additional information about a received {@link Packet}.
 */
public interface PacketContext {

	/**
	 * The {@link NetworkID} of the network manager that received the packet.
	 * @return The local NetworkID
	 */
	public NetworkID getLocalID();
	
	/**
	 * The {@link NetworkID} that identifies the packets source; the remote side of
	 * the network connection over which the packet was sent.
	 * @return The remote NetworkID
	 */
	public NetworkID getRemoteID();
	
	/**
	 * Whether the packet was received by a server.
	 * @return {@code true} if the packet was received by a server, {@code false} if it was received by a client
	 */
	public boolean isServer();
	
	/**
	 * Sends a new {@link Packet} back over the same connection to the source.
	 * @param packet The Packet to send back
	 */
	public void replyPacket(Packet packet);
}
