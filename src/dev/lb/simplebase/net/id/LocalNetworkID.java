package dev.lb.simplebase.net.id;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.ValueType;

/**
 * Internal class. See {@link NetworkID} for proper documentation.
 */
@ValueType
@Internal
class LocalNetworkID extends NetworkID {
	//Stores the NetworkIDs description; immutable
	
	protected LocalNetworkID(String description) {
		super(description);
	}

	@Override
	public boolean hasFeature(NetworkIDFeature<?> function) {
		//Only return true if the function is LOCAL.
		return function == NetworkIDFeature.INTERNAL;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> E getFeature(NetworkIDFeature<E> function) {
		if(function == NetworkIDFeature.INTERNAL) {
			return (E) description;
		} else {
			throw new UnsupportedOperationException("Unsupported NetworkID function: " + function);
		}
	}
	
	@Override
	public String toString(boolean includeDetails) {
		return "NetworkID: Description:" + description + (includeDetails ? ", Functions:[LOCAL]" : "");
	}

	@Override
	public String toString() {
		return "LocalNetworkID [description=" + description + "]";
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj) && obj instanceof LocalNetworkID;
	}

	@Override
	public NetworkID clone() {
		return new LocalNetworkID(description);
	}

	@Override
	public NetworkID cloneWith(String newDescription) {
		return new LocalNetworkID(newDescription);
	}
}
