package dev.lb.simplebase.net.config;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;

import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.NetworkManagerServer;

/**
 * The type of {@link NetworkManagerServer} that will be created with a config.
 * <p>
 * To have a client successfully connect to the server, the client must have a compatible {@link ConnectionType};
 * and the server has to be created using a compatible {@link NetworkID}:
 * </p>
 * <table>
 * <caption>{@code ServerType} and {@code ConnectionType} compatibility:</caption>
 * <tr><th>{@code ServerType}</th><th>Client {@code ConnectionType}</th><th>Required {@link NetworkIDFunction}</th></tr>
 * <tr><td>{@link #INTERNAL}</td><td>{@link ConnectionType#INTERNAL}</td><td>{@link NetworkIDFunction#INTERNAL}</td></tr>
 * <tr><td>{@link #TCP_IO} or {@link #TCP_NIO}</td><td>{@link ConnectionType#TCP}</td><td>{@link NetworkIDFunction#BIND}</td></tr>
 * <tr><td>{@link #UDP_IO} or {@link #UDP_NIO}</td><td>{@link ConnectionType#UDP}</td><td>{@link NetworkIDFunction#BIND}</td></tr>
 * <tr><td>{@link #COMBINED_IO} or {@link #COMBINED_NIO}</td><td>{@link ConnectionType#TCP} or {@link ConnectionType#UDP}</td>
 * <td>{@link NetworkIDFunction#BIND}</td></tr>
 * </table>
 * <h2>Special case: {@link #DEFAULT}</h2>
 * <p>
 * The server type created when using the {@code DEFAULT} connection type is determined at runtime depending on
 * the {@link NetworkID} used to create the client. It is converted into a resolved {@code ServerType} using
 * the {@link #resolve(ServerType, NetworkID)} method.
 * </p>
 */
public enum ServerType {

	/**
	 * Will be resolved with {@link #resolve(ServerType, NetworkID)} before creating a client. Will choose between
	 * {@link #INTERNAL} and {@link #TCP_IO} depending on the implemented functions of the used {@link NetworkID}.
	 */
	DEFAULT(false, false, false),
	/**
	 * The server only supports internal connections.
	 * <p>If the {@code ServerConfig.getRegisterInternalServer()} value is set to {@code false}
	 * and this ServerType is used, the server instance would be effectively unusable.<br>
	 * For this reason an exception is thrown at server creation time when the type is {@link #INTERNAL}
	 * and {@link ServerConfig#getRegisterInternalServer()} is {@code false}. 
	 */
	INTERNAL(false, false, false),
	/**
	 * The server supports TCP ({@link Socket}/{@link ServerSocket}) connections using blocking IO.
	 * UDP connections are not supported.
	 * <p>
	 * The server will also support internal connections if
	 * {@link ServerConfig#getRegisterInternalServer()} is set to {@code true}
	 * </p>
	 * <p>
	 * The server might still bind to a UDP port if {@link ServerConfig#getServerInfoPacket()} is
	 * configured to respond to info packet requests
	 * </p>
	 */
	TCP_IO(true, false, true),
	/**
	 * The server supports TCP ({@link SocketChannel}/{@link ServerSocketChannel}) connections using non-blocking IO.
	 * UDP connections are not supported.
	 * <p>
	 * The server will also support internal connections if
	 * {@link ServerConfig#getRegisterInternalServer()} is set to {@code true}.
	 * </p>
	 * <p>
	 * The server might still bind to a UDP port if {@link ServerConfig#getServerInfoPacket()} is
	 * configured to respond to info packet requests.
	 * </p>
	 */
	TCP_NIO(false, false, true),
	/**
	 * The server supports UDP ({@link DatagramSocket}) connections using blocking IO.
	 * TCP connections are not supported.
	 * <p>
	 * The server will also support internal connections if
	 * {@link ServerConfig#getRegisterInternalServer()} is set to {@code true}.
	 * </p>
	 * <p>
	 * If {@link ServerConfig#getServerInfoPacket()} is configured to respond to info packet requests,
	 * they will use the same socket as regular connections.
	 * </p>
	 */
	UDP_IO(true, true, false),
	/**
	 * The server supports UDP ({@link DatagramChannel}) connections using non-blocking IO.
	 * TCP connections are not supported.
	 * <p>
	 * The server will also support internal connections if
	 * {@link ServerConfig#getRegisterInternalServer()} is set to {@code true}.
	 * </p>
	 * <p>
	 * If {@link ServerConfig#getServerInfoPacket()} is configured to respond to info packet requests,
	 * they will use the same socket as regular connections.
	 * </p>
	 */
	UDP_NIO(false, true, false),
	/**
	 * The server supports both UDP ({@link DatagramSocket}) and
	 * TCP ({@link Socket}/{@link ServerSocket}) connections using blocking IO.
	 * <p>
	 * The server will also support internal connections if
	 * {@link ServerConfig#getRegisterInternalServer()} is set to {@code true}.
	 * </p>
	 * <p>
	 * If {@link ServerConfig#getServerInfoPacket()} is configured to respond to info packet requests,
	 * they will use the same UDP channel as regular connections.
	 * </p>
	 */
	COMBINED_IO(true, true, true),
	/**
	 * The server supports both UDP ({@link DatagramChannel}) and
	 * TCP ({@link SocketChannel}/{@link ServerSocketChannel}) connections using non-blocking IO.
	 * <p>
	 * The server will also support internal connections if
	 * {@link ServerConfig#getRegisterInternalServer()} is set to {@code true}.
	 * </p>
	 * <p>
	 * If {@link ServerConfig#getServerInfoPacket()} is configured to respond to info packet requests,
	 * they will use the same UDP channel as regular connections.
	 * </p>
	 */
	COMBINED_NIO(false, true, true);
	
	private final boolean useSockets;
	private final boolean udp;
	private final boolean tcp;
	
	private ServerType(boolean useSockets, boolean udp, boolean tcp) {
		this.useSockets = useSockets;
		this.udp = udp;
		this.tcp = tcp;
	}
	
	/**
	 * {@code True} if a server constructed with this {@link ServerType} will do blocking IO using the
	 * API in {@code java.io}.<br>
	 * {@code False} if non-blocking IO is used or the server is internal only.
	 * @return {@code true} if this server type uses sockets, false otherwise
	 * @see #useChannels()
	 * @see #isInternal()
	 */
	public boolean useSockets() {
		return useSockets;
	}
	
	/**
	 * {@code True} if a server constructed with this {@link ServerType} will do non-blocking IO using the
	 * API in {@code java.nio}.<br>
	 * {@code False} if blocking IO is used or the server is internal only.
	 * @return {@code true} if this server type uses channels, false otherwise
	 * @see #useSockets()
	 * @see #isInternal()
	 */
	public boolean useChannels() {
		return !useSockets && !isInternal();
	}
	
	/**
	 * {@code True} if a server constructed with this {@link ServerType} will be internal-only
	 * and not do any IO.<br>
	 * {@code False} if any type of IO, blocking or non-blocking is used.
	 * <p>
	 * A server that does IO can still be available as an internal server if {@link ServerConfig#getRegisterInternalServer()}
	 * is enabled, but that does not count as internal-only and for such servers, this method returns {@code false}.
	 * </p>
	 * @return {@code true} if this server type uses no IO, false otherwise
	 * @see #useSockets()
	 * @see #useChannels()
	 */
	public boolean isInternal() {
		return !(useSockets || udp || tcp);
	}
	
	/**
	 * Whether this {@link ServerType} is resolved and a valid server can created with it.
	 * <p>
	 * An unresolved {@code ServerType} must be passed to {@link #resolve(ServerType, NetworkID)} before it can be used.
	 * This will be done automatically by {@code NetworkManager}'s factory methods. Unresolved types can be 
	 * safely used as a {@link ServerType} in {@link ServerConfig#setServerType(ServerType)}.
	 * </p>
	 * @return Whether this server type is resolved
	 */
	public boolean isResolved() {
		return this != DEFAULT;
	}
	
	/**
	 * Whether a server created with this type will support UDP / Datagram connections from clients.
	 * <p>
	 * Even if this method returns {@code false}, the server might still create a Datagram socket to
	 * respond to server info requests, if enabled in {@link ServerConfig#getServerInfoPacket()}.
	 * </p>
	 * @return Whether this server type supports UDP connections
	 */
	public boolean supportsUdp() {
		return udp;
	}
	
	/**
	 * Whether a server created with this type will support TCP connections from clients.
	 * @return Whether this server type supports TCP connections
	 */
	public boolean supportsTcp() {
		return tcp;
	}
	
	private static final ServerType NET_DEFAULT = TCP_IO;
	
	/**
	 * Resolves the actual server type to use depending on
	 * the server {@link NetworkID}, and throws an exception if
	 * those are incompatible.
	 * <p>
	 * Used internally to choose a network manager implementation.
	 * </p>
	 * @param type The server type that should be resolved
	 * @param id The {@link NetworkID} that holds the server local address
	 * @return The resolved server type; this can be any type except {@link #DEFAULT}
	 * and will never be {@code null}
	 * @throws NullPointerException If {@code type} or {@code id} are {@code null}
	 * @throws IllegalArgumentException If the {@link NetworkID} implementation and the {@link ServerType} are incompatible
	 */
	public static ServerType resolve(ServerType type, NetworkID id) {
		Objects.requireNonNull(type, "'type' parameter must not be null");
		Objects.requireNonNull(id, "'id' parameter must not be null");
		
		if(type == DEFAULT) {
			if(id.hasFunction(NetworkIDFunction.INTERNAL)) return INTERNAL;
			if(id.hasFunction(NetworkIDFunction.NETWORK)) return NET_DEFAULT;
			throw new IllegalArgumentException("Server NetworkID must either implement INTERNAL or NETWORK function");
		} else if(type == INTERNAL) {
			if(!id.hasFunction(NetworkIDFunction.INTERNAL))
				throw new IllegalArgumentException("Server NetworkID must implement INTERNAL if that server type is used");
		} else { //Any network
			if(!id.hasFunction(NetworkIDFunction.NETWORK) || !id.hasFunction(NetworkIDFunction.BIND))
				throw new IllegalArgumentException("Server NetworkID must implement NETWORK and BIND if any network server type is used");
		}
		return type;
	}
}
