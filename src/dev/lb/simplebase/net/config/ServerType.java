package dev.lb.simplebase.net.config;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;

import dev.lb.simplebase.net.NetworkManagerServer;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;

/**
 * Sets the type of connections that a {@link NetworkManagerServer} will be able to accept.
 */
public enum ServerType {

	/**
	 * Will choose between {@link #INTERNAL} and {@link #TCP_SOCKET} depending on the used {@link NetworkID}.
	 */
	DEFAULT,
	/**
	 * The server only supports internal connections.<p>
	 * If the {@code ServerConfig.getRegisterInternalServer()} value is set to {@code false}
	 * and this ServerType is used, the server instance would be effectively unusable.<br>
	 * For this reason an exception is thrown at server creation time when the type is {@link #INTERNAL}
	 * and {@link ServerConfig#getRegisterInternalServer()} is {@code false}. 
	 */
	INTERNAL,
	/**
	 * The server supports TCP ({@link Socket}/{@link ServerSocket}) connections, and not UDP
	 * connections.<p>
	 * The server will also support internal connections if
	 * {@link ServerConfig#getRegisterInternalServer()} is set to {@code true}
	 * <br>
	 * The server will bind to a UDP port if {@link ServerConfig#getAllowDetection()} if {@code true},
	 * but no UDP connections can be established.
	 */
	TCP_SOCKET,
	/**
	 * The server supports TCP ({@link SocketChannel}/{@link ServerSocketChannel}) connections, and not UDP
	 * connections.<p>
	 * The server will also support internal connections if
	 * {@link ServerConfig#getRegisterInternalServer()} is set to {@code true}
	 * <br>
	 * The server will bind to a UDP port if {@link ServerConfig#getAllowDetection()} if {@code true},
	 * but no UDP connections can be established.
	 */
	TCP_CHANNEL,
	/**
	 * The server will support UDP ({@link DatagramSocket}) connections, and not TCP connections.<p>
	 * The server will also support internal connections if
	 * {@link ServerConfig#getRegisterInternalServer()} is set to {@code true}
	 * <p>
	 * Because UDP connections are unmanaged, it is recommended to enable {@link CommonConfig#getGlobalConnectionCheck()}
	 * to periodically check connection state.
	 */
	UDP_SOCKET,
	/**
	 * The server will support UDP ({@link DatagramChannel}) connections, and not TCP connections.<p>
	 * The server will also support internal connections if
	 * {@link ServerConfig#getRegisterInternalServer()} is set to {@code true}
	 * <p>
	 * Because UDP connections are unmanaged, it is recommended to enable {@link CommonConfig#getGlobalConnectionCheck()}
	 * to periodically check connection state.
	 */
	UDP_CHANNEL,
	/**
	 * The server will support both UDP and TCP connections (See {@link #TCP_SOCKET} and {@link #UDP_SOCKET}) at the same time.
	 */
	COMBINED_SOCKET,
	/**
	 * The server will support both UDP and TCP connections (See {@link #TCP_CHANNEL} and {@link #UDP_CHANNEL}) at the same time.
	 */
	COMBINED_CHANNEL;
	
	private static final ServerType NET_DEFAULT = TCP_SOCKET;
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
			if(!id.hasFunction(NetworkIDFunction.NETWORK))
				throw new IllegalArgumentException("Server NetworkID must implement NETWORK if any network server type is used");
		}
		return type;
	}
}