package dev.lb.simplebase.net.id;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.ValueType;

/**
 * Internal class. See {@link NetworkID} for proper documentation.
 */
@ValueType
@Internal
class BindNetworkID extends NetworkID {
	private final int port;
	
	protected BindNetworkID(String description, int port) {
		super(description);
		this.port = port;
	}

	@Override
	public boolean hasFeature(NetworkIDFeature<?> function) {
		return function == NetworkIDFeature.NETWORK || function == NetworkIDFeature.BIND;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <E> E getFeature(NetworkIDFeature<E> function) {
		if(function == NetworkIDFeature.NETWORK) {
			return (E) NetworkIDFeature.BIND;
		} else if(function == NetworkIDFeature.BIND) {
			return (E) getBindAddress();
		} else {
			throw new UnsupportedOperationException("Unsupported NetworkID function: " + function);
		}
	}

	protected SocketAddress getBindAddress() {
		return new InetSocketAddress((InetAddress) null, port);
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
	public NetworkID cloneWith(String newDescription) {
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
