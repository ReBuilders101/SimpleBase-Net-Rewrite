package dev.lb.simplebase.net.id;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * Depending on how a {@link NetworkID} was created an what network object it represents,
 * it can have different features. A {@code NetworkID} can have more than one feature, but not all combinations are possible.
 * <p>
 * Every {@link NetworkIDFeature} encodes a value for a NetworkID if present. It
 * can be accessed through {@link NetworkID#getFeature(NetworkIDFeature)}.
 * </p>
 * @param <E> The type of the encoded value
 */
public final class NetworkIDFeature<E> {
	
	/**
	 * The NetworkID has no actual networking information and only serves as a label for a network object. Usually used
	 * for the local side of a connection.<br>Cannot be combined with any other NetworkIDFunction.
	 * <p> 
	 * Its parameter is the description string of the NetworkID, as this is the only information
	 * </p>
	 * stored by a local ID.
	 */
	public static final NetworkIDFeature<String> INTERNAL = new NetworkIDFeature<>("INTERNAL");
	
	/**
	 * The NetworkID contains any kind networking data. This NetworkIDFunction is usually combined with either
	 * {@link #CONNECT} or {@link #BIND} to specify the type of network information present.
	 * <p>
	 * Its parameter is the {@link NetworkIDFeature} it is combined with, either {@link #CONNECT} or
	 * {@link #BIND}.
	 * </p>
	 */
	public static final NetworkIDFeature<NetworkIDFeature<InetSocketAddress>> NETWORK = new NetworkIDFeature<>("NETWORK");
	
	/**
	 * The NetworkID contains all networking information necessary to connect a socket to a remote endpoint.
	 * Always combined with {@link #NETWORK}.
	 * <p>
	 * Its parameter is the the address that the socket should be connected to.
	 * </p>
	 */
	public static final NetworkIDFeature<InetSocketAddress> CONNECT = new NetworkIDFeature<>("CONNECT");
	
	/**
	 * The NetworkID contains all networking information necessary to bind a server socket to a local endpoint.
	 * <p>
	 * Its parameter is the address that the socket should be bound to.
	 * </p>
	 */
	public static final NetworkIDFeature<InetSocketAddress> BIND = new NetworkIDFeature<>("BIND");
	
	private final String textDescription;
	
	//No other instances
	private NetworkIDFeature(String textDescription) {
		this.textDescription = textDescription;
	}

	@Override
	public int hashCode() {
		return Objects.hash(textDescription);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public String toString() {
		return textDescription;
	}
}
