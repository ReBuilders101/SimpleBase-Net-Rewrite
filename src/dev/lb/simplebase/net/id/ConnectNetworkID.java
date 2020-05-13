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
class ConnectNetworkID extends NetworkID {
	//Only allow InetSocketAddress as a SocketAddress implementation
	private final InetSocketAddress address;
	
	/**
	 * Internal constructor. Use {@link NetworkManager} to create instances
	 * Params must not be null!
	 */
	protected ConnectNetworkID(String description, InetSocketAddress address) {
		super(description);
		this.address = address;
	}
	
	/**
	 * The address that can be used to connect a socket to an endpoint.
	 * @return The {@link SocketAddress} for this NetworkID
	 */
	public SocketAddress getConnectAddress() {
		return address;
	}
	
	@Override
	public boolean hasFunction(NetworkIDFunction function) {
		//Implements NETWORK and CONNECT
		return function == NetworkIDFunction.NETWORK || function == NetworkIDFunction.CONNECT;
	}

	@Override
	public String toString() {
		return "ConnectNetworkID [address=" + address + ", description=" + description + "]";
	}

	@Override
	public String toString(boolean includeDetails) {
		return "NetworkID: Description:" + description + (includeDetails ? ", Functions:[NETWORK, CONNECT], Address/IP:"
				+ address.getHostString() + ", Port: " + address.getPort() : "");
	}

	@Override
	public NetworkID clone() {
		return new ConnectNetworkID(description, address);
	}

	@Override
	public NetworkID clone(String newDescription) {
		return new ConnectNetworkID(newDescription, address);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(address);
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
		if (!(obj instanceof ConnectNetworkID)) {
			return false;
		}
		ConnectNetworkID other = (ConnectNetworkID) obj;
		return Objects.equals(address, other.address);
	}
}
