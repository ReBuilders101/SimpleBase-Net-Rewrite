package dev.lb.simplebase.net.packet;

import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.id.NetworkID;

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
	 * @return See {@link NetworkConnection#sendPacket(Packet)} for return value details
	 */
	public boolean replyPacket(Packet packet);
	
	/**
	 * A custom object associated with this connection that has been set by the user.
	 * @return The custom user object
	 */
	public Object getCustomData();
	
	/**
	 * A custom object associated with this connection that has been set by the user,
	 * casted to the correct type.
	 * @param <T> The type of the custom object
	 * @param dataType The type class of the custom object
	 * @return The custom object casted to that type
	 * @throws ClassCastException If the requested type and the actual type of the object are incompatible
	 */
	public <T> T getCustomData(Class<T> dataType);
}
