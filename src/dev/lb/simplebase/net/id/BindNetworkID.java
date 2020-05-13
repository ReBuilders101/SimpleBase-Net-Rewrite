package dev.lb.simplebase.net.id;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.ValueType;

/**
 * Internal class. See {@link NetworkID} for proper documentation.
 */
@ValueType
@Internal
class BindNetworkID extends NetworkID {
	private final int port;
	
	/**
	 * Internal constructor. Use {@link NetworkManager} to create instances
	 * Params must not be null!
	 */
	protected BindNetworkID(String description, int port) {
		super(description);
		this.port = port;
	}

	@Override
	public boolean hasFunction(NetworkIDFunction function) {
		return function == NetworkIDFunction.NETWORK || function == NetworkIDFunction.BIND;
	}

	public SocketAddress getBindAddress() {
		return new InetSocketAddress(port);
	}
	
	@Override
	public String toString(boolean includeDetails) {
		return "NetworkID: Description:" + description + (includeDetails ? ", Functions:[NETWORK, BIND], Port: " + port : "");
	}

	@Override
	public String toString() {
		return "BindNetworkID [port=" + port + ", description=" + description + "]";
	}

	@Override
	public NetworkID clone() {
		return new BindNetworkID(description, port);
	}

	@Override
	public NetworkID clone(String newDescription) {
		return new BindNetworkID(newDescription, port);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(port);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof BindNetworkID)) {
			return false;
		}
		BindNetworkID other = (BindNetworkID) obj;
		return port == other.port;
	}

}
