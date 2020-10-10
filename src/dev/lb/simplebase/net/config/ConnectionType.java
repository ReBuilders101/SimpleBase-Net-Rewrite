package dev.lb.simplebase.net.config;

import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Objects;

import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFeature;
import dev.lb.simplebase.net.manager.NetworkManagerClient;

/**
 * <p>
 * The type of network connection that a {@link NetworkManagerClient} will make when connecting to the server.
 * </p>
 * <h2>Server/Client type compatibility</h2>
 * <p>
 * To successfully connect to a server, the server must have a compatible {@link ServerType}; and the client has to be created
 * using a compatible {@link NetworkID}:
 * </p>
 * <table>
 * <caption>{@code ConnectionType} and {@code ServerType} compatibility:</caption>
 * <tr><th>{@code ConnectionType}</th><th>Server requirements</th><th>Required {@link NetworkIDFeature}</th></tr>
 * <tr><td>{@link #INTERNAL}</td><td>Any server that enables
 * 		{@link ServerConfig#getRegisterInternalServer()}</td><td>{@link NetworkIDFeature#INTERNAL}</td></tr>
 * <tr><td>{@link #TCP}</td><td>{@link ServerType#TCP_IO}, {@link ServerType#TCP_NIO}, {@link ServerType#COMBINED_IO},
 * 		{@link ServerType#COMBINED_NIO}</td><td>{@link NetworkIDFeature#CONNECT}</td></tr>
 * <tr><td>{@link #UDP}</td><td>{@link ServerType#UDP_IO}, {@link ServerType#UDP_IO}, {@link ServerType#COMBINED_IO},
 * 		{@link ServerType#COMBINED_NIO}</td><td>{@link NetworkIDFeature#CONNECT}</td></tr>
 * </table>
 * <h2>Special case: {@link #DEFAULT}</h2>
 * <p>
 * The connection type created when using the {@code DEFAULT} connection type is determined at runtime depending on
 * the {@link NetworkID} used to create the client. It is converted into a resolved {@code ConnectionType} using
 * the {@link #resolve(ConnectionType, NetworkID)} method.
 * </p>
 */
public enum ConnectionType {
	/**
	 * Will be resolved with {@link #resolve(ConnectionType, NetworkID)} before creating a client. Will choose between
	 * {@link #INTERNAL} and {@link #TCP} depending on the implemented functions of the used remote {@link NetworkID}.
	 */
	DEFAULT,
	/**
	 * Creates an internal connection. The used server {@link NetworkID} must 
	 * implement {@link NetworkIDFunction#INTERNAL}.
	 */
	INTERNAL,
	/**
	 * Uses a {@link Socket} to establish a TCP connection to the server.
	 * The used remote {@link NetworkID} must implement {@link NetworkIDFunction#CONNECT}.<br>
	 * The server must be compatible according to the table in the class description.
	 */
	TCP,
	/**
	 * Uses a {@link DatagramSocket} to establish a UDP connection to the server.
	 * The used remote {@link NetworkID} must implement {@link NetworkIDFunction#CONNECT}.<br>
	 * The server must be compatible according to the table in the class description.
	 */
	UDP;
	
	
	private static final ConnectionType NET_DEFAULT = TCP; //Change javadoc if changing this value
	
	/**
	 * Whether this {@link ConnectionType} is resolved and a valid connection can created with it.
	 * <p>
	 * An unresolved {@code ConnectionType} must be passed to {@link #resolve(ConnectionType, NetworkID)} before it can be used.
	 * This will be done automatically by {@code NetworkManager}'s factory methods. Unresolved types can be 
	 * safely used as a {@link ConnectionType} in {@link ClientConfig#setConnectionType(ConnectionType)}.
	 * </p>
	 * @return Whether this connection type is resolved
	 */
	public boolean isResolved() {
		return this != DEFAULT;
	}
	
	/**
	 * Checks whether a connection made with this {@link ConnectionType} to a server using the supplied
	 * {@link ServerConfig} can be successful.
	 * <p>
	 * If {@code config} is {@code null}, this method returns {@code false}. If {@link #isResolved()}
	 * returns {@code false} for this {@code ConnectionType}, this method will also return {@code false}.
	 * @param config The {@link ServerConfig} used for the server to which compatibility should be checked
	 * @return Whether a connection using this {@link ConnectionType} will be successful
	 */
	public boolean canConnectTo(ServerConfig config) {
		if(config == null) return false;
		if(this == DEFAULT) return false;
		if(this == INTERNAL) return config.getRegisterInternalServer();
		if(this == TCP) return config.getServerType().supportsTcp();
		if(this == UDP) return config.getServerType().supportsUdp();
		return false;
	}
	
	/**
	 * Resolves the actual connection type to use depending on
	 * the server {@link NetworkID}, and throws an exception if
	 * those are incompatible.
	 * <p>
	 * Used internally to choose a network manager implementation.
	 * </p>
	 * @param type The connection type that should be resolved
	 * @param id The {@link NetworkID} that holds the server remote address
	 * @return The resolved {@link ConnectionType}; this can be any type except {@link #DEFAULT}
	 * and will never be {@code null}
	 * @throws NullPointerException If {@code type} or {@code id} are {@code null}
	 * @throws IllegalArgumentException If the {@link NetworkID} implementation and the {@link ConnectionType} are incompatible
	 */
	public static ConnectionType resolve(ConnectionType type, NetworkID id) {
		Objects.requireNonNull(type, "'type' parameter must not be null");
		Objects.requireNonNull(id, "'id' parameter must not be null");
		if(type == DEFAULT) {
			if(id.hasFeature(NetworkIDFeature.INTERNAL)) return INTERNAL;
			if(id.hasFeature(NetworkIDFeature.NETWORK)) return NET_DEFAULT;
			throw new IllegalArgumentException("Server NetworkID must either implement INTERNAL or NETWORK function");
		} else if(type == INTERNAL) {
			if(!id.hasFeature(NetworkIDFeature.INTERNAL))
				throw new IllegalArgumentException("Server NetworkID must implement INTERNAL if that connection type is used");
		} else { //Any network
			if(!id.hasFeature(NetworkIDFeature.NETWORK))
				throw new IllegalArgumentException("Server NetworkID must implement NETWORK if any network connection type is used");
		}
		return type;
	}
}