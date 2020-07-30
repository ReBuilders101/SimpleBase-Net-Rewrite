package dev.lb.simplebase.net.config;

import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;

import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;

/**
 * The type of network connection to make to the server.<br>
 * Depending on the used server {@link NetworkID}, not all types may be valid.
 */
public enum ConnectionType {
	/**
	 * The default value. Will choose between {@link #INTERNAL} and {@link #TCP_SOCKET}
	 * depending on the implemented functions of the used server {@link NetworkID}.
	 */
	DEFAULT,
	/**
	 * Creates an internal connection. The used server {@link NetworkID} must 
	 * implement {@link NetworkIDFunction#INTERNAL}.
	 */
	INTERNAL,
	/**
	 * Uses a {@link Socket} to establish a TCP connection to the server.
	 * The used server {@link NetworkID} must implement {@link NetworkIDFunction#NETWORK}.<br>
	 * The server must be able to accept TCP connections.
	 */
	TCP_SOCKET,
	/**
	 * Uses a {@link SocketChannel} to establish a TCP connection to the server.
	 * The used server {@link NetworkID} must implement {@link NetworkIDFunction#NETWORK}.<br>
	 * The server must be able to accept TCP connections.
	 */
	TCP_CHANNEL,
	/**
	 * Uses a {@link DatagramSocket} to establish a UDP connection to the server.
	 * The used server {@link NetworkID} must implement {@link NetworkIDFunction#NETWORK}.<br>
	 * The server must be able to accept UDP connections.
	 */
	UDP_SOCKET,
	/**
	 * Uses a {@link DatagramChannel} to establish a UDP connection to the server.
	 * The used server {@link NetworkID} must implement {@link NetworkIDFunction#NETWORK}.<br>
	 * The server must be able to accept UDP connections.
	 */
	UDP_CHANNEL;
	
	
	private static final ConnectionType NET_DEFAULT = TCP_SOCKET; //Change javadoc if changing this value
	
	/**
	 * Resolves the actual connection type to use depending on
	 * the server {@link NetworkID}, and throws an exception if
	 * those are incompatible.
	 * <p>
	 * Used internally to choose a network manager implementation,
	 * users should not use this method themselves when choosing an implementation.
	 * @param type The connection type that should be resolved
	 * @param id The {@link NetworkID} that holds the server address
	 * @return The resolved {@link ConnectionType}; this can be any type except {@link #DEFAULT}
	 * and will never be {@code null}
	 * @throws NullPointerException If {@code type} or {@code id} are null
	 * @throws IllegalArgumentException If the {@link NetworkID} implementation and the {@link ConnectionType} are incompatible
	 */
	public static ConnectionType resolve(ConnectionType type, NetworkID id) {
		Objects.requireNonNull(type, "'type' parameter must not be null");
		Objects.requireNonNull(id, "'id' parameter must not be null");
		if(type == DEFAULT) {
			if(id.hasFunction(NetworkIDFunction.INTERNAL)) return INTERNAL;
			if(id.hasFunction(NetworkIDFunction.NETWORK)) return NET_DEFAULT;
			throw new IllegalArgumentException("Server NetworkID must either implement INTERNAL or NETWORK function");
		} else if(type == INTERNAL) {
			if(!id.hasFunction(NetworkIDFunction.INTERNAL))
				throw new IllegalArgumentException("Server NetworkID must implement INTERNAL if that connection type is used");
		} else { //Any network
			if(!id.hasFunction(NetworkIDFunction.NETWORK))
				throw new IllegalArgumentException("Server NetworkID must implement NETWORK if any network connection type is used");
		}
		return type;
	}
}