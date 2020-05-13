package dev.lb.simplebase.net.id;

import java.net.SocketAddress;

import dev.lb.simplebase.net.annotation.Internal;

/**
 * Depending on how a {@link NetworkID} was created an what network object it represents,
 * it can have different functions. A NetworkID can have more than one function, but not all combinations are possible.
 */
public enum NetworkIDFunction {
	
	/**
	 * The NetworkID has no actual networking information and only serves as a label for a network object. Usually used
	 * for the local side of a connection.<br>Cannot be combined with any other NetworkIDFunction. 
	 */
	LOCAL,
	
	/**
	 * The NetworkID contains any kind networking data. This NetworkIDFunction is usually combined with either
	 * {@link #CONNECT} or {@link #BIND} to specify the type of network information present.
	 */
	NETWORK,
	
	/**
	 * The NetworkID contains all networking information necessary to connect a socket to a remote endpoint.
	 * Always combined with {@link #NETWORK}.
	 */
	CONNECT,
	
	/**
	 * The NetworkID contains all networking information necessary to bind a server socket to a local endpoint.
	 */
	BIND;
	
	/**
	 * Internal utiliy method
	 */
	@Internal
	protected SocketAddress connectAddressOrNull(NetworkID id) {
		return id instanceof ConnectNetworkID ? ((ConnectNetworkID) id).getConnectAddress() : null;
	}
	
	/**
	 * Internal utiliy method
	 */
	@Internal
	protected SocketAddress bindAddressOrNull(NetworkID id) {
		return id instanceof BindNetworkID ? ((BindNetworkID) id).getBindAddress() : null;
	}
}
