package dev.lb.simplebase.net.id;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.ValueType;

/**
 * Internal class. See {@link NetworkID} for proper documentation.
 */
@ValueType
@Internal
class LocalNetworkID extends NetworkID {
	//Stores the NetworkIDs description; immutable
	
	/**
	 * Internal constructor. Use {@link NetworkManager} to create instances.
	 * Param must not be null!
	 */
	protected LocalNetworkID(String description) {
		super(description);
	}

	@Override
	public boolean hasFunction(NetworkIDFunction function) {
		//Only return true if the function is LOCAL.
		return function == NetworkIDFunction.LOCAL;
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
	public NetworkID clone(String newDescription) {
		return new LocalNetworkID(newDescription);
	}

}
