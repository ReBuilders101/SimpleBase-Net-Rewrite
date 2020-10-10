package dev.lb.simplebase.net.id;

import java.net.InetSocketAddress;
import java.util.Objects;

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
	
	protected ConnectNetworkID(String description, InetSocketAddress address) {
		super(description);
		this.address = address;
	}
	
	@Override
	public boolean hasFeature(NetworkIDFeature<?> function) {
		//Implements NETWORK and CONNECT
		return function == NetworkIDFeature.NETWORK || function == NetworkIDFeature.CONNECT;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <E> E getFeature(NetworkIDFeature<E> function) {
		if(function == NetworkIDFeature.NETWORK) {
			return (E) NetworkIDFeature.CONNECT;
		} else if(function == NetworkIDFeature.CONNECT) {
			return (E) address;
		} else {
			throw new UnsupportedOperationException("Unsupported NetworkID function: " + function);
		}
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
	public NetworkID cloneWith(String newDescription) {
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
