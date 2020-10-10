package dev.lb.simplebase.net.id;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.function.Function;

import dev.lb.simplebase.net.annotation.Immutable;
import dev.lb.simplebase.net.annotation.ValueType;
import dev.lb.simplebase.net.util.Cloneable2;

/**
 * An Instance of NetworkID represents a network party and contains all necessary information to make a connection.
 * Instances can be created through static methods in this class.
 */
@ValueType
@Immutable
public abstract class NetworkID implements Cloneable2 {

	protected final String description;
	
	protected NetworkID(String description) {
		this.description = description;
	}
	
	/**
	 * A text description/identifier for this NetworkID. Usually used as an unique identifier, but
	 * uniqueness is not guaranteed by the API and must be enforced by the API user (e.g. by using
	 * {@link #isUnique(Iterable, String)} before cretaing the new ID).
	 * @return The description text for this NetworkID
	 */
	public final String getDescription() {
		return description;
	}
	
	/**
	 * Checks whether this NetworkID implements the requested optional feature.
	 * No NetworkID can implement the {@code null} feature, so this method always returns {@code false}
	 * if the parameter has the value {@code null}.
	 * @param feature The optional feature that this NetworkID might implement
	 * @return {@code true} if this {@link NetworkID} implementation supports the requested feature, {@code false} otherwise
	 */
	public abstract boolean hasFeature(NetworkIDFeature<?> feature);
	
	/**
	 * Gets the value of a feature if that feature is implemented by this {@link NetworkID}.
	 * <p>
	 * To check whether a certain feature is supported by this {@code NetworkID}, {@link #hasFeature(NetworkIDFeature)}
	 * can be used. Will throw an exception if the requested feature is not implemented.
	 * </p>
	 * @param <E> The type of the value for the feature
	 * @param feature The the feature that holds the value
	 * @return The value of the requested feature
	 * @throws UnsupportedOperationException If the requested feature is not implemented
	 */
	public abstract <E> E getFeature(NetworkIDFeature<E> feature);
	
	/**
	 * Runs the action only if the requested optional feature is implemented by this {@link NetworkID}.
	 * @param <E> The type of the value for the feature
	 * @param <R> The return type of the action
	 * @param feature The feature that this {@code NetworkID} might implement
	 * @param action The action to run with the feature's value if the feature is present
	 * @param otherwise The value to return if the feature is not present
	 * @return The return value of the action, or the value of the {@code otherwise} parameter
	 */
	public <E, R> R ifFeature(NetworkIDFeature<E> feature, Function<E, R> action, R otherwise) {
		if(hasFeature(feature)) {
			return action.apply(getFeature(feature));
		} else {
			return otherwise;
		}
	}
	
	/**
	 * An alternate {@link #toString()} implementation. While the normal {@code toString} method lists all member values
	 * and the implementation type for debugging, this method can be used to generate more human-readable descriptions.
	 * It will always contain the NetworkID's description string, and if the parameter {@code includeDetails} is true,
	 * optional networking information.
	 * @param includeDetails Whether the network details beside the description are included in the string
	 * @return A configurable String representation of this {@link NetworkID}
	 */
	public abstract String toString(boolean includeDetails);

	
	@Override
	public abstract NetworkID clone();
	
	@Override
	public final NetworkID copy() {
		//All IDs are immutable, so this is valid
		return this;
	}
	
	/**
	 * Creates a new {@link NetworkID} that implements the same features with the same values,
	 * but has a different description.
	 * @param newDescription The new description for the cloned NetworkID instance.
	 * @return A new NetworkID with identical values except for the description
	 */
	public abstract NetworkID cloneWith(String newDescription);
	
	@Override
	public int hashCode() {
		return Objects.hash(description);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof NetworkID)) {
			return false;
		}
		NetworkID other = (NetworkID) obj;
		return Objects.equals(description, other.description);
	}
	
	/**
	 * Tries to find an existing NetworkID in a list that matches the description. If more than one NetworkID
	 * with a matching description is present in the list, the first one is returned.
	 * @param source The list / array / other collection that should be searched for the description 
	 * @param description The description of the NetworkID that should be found
	 * @return The NetworkID with a matching description, or {@code null} if no matching NetworkID was found
	 * @throws NullPointerException If {@code source} or {@code description} are {@code null}
	 */
	public static NetworkID find(Iterable<NetworkID> source, String description) {
		Objects.requireNonNull(source, "'source' parameter must not be null");
		Objects.requireNonNull(description, "'description' parameter must not be null");
		for(NetworkID id : source) {
			if(id == null) continue; //Skip null entries in the source.
			if(id.getDescription().equals(description)) {
				return id; //return the ID with the same name
			}
		}
		return null; //Nothing was found
	}
	
	/**
	 * Checks whether a NetworkID with a matching description already exists in the list.
	 * @param source The list / array / other collection that should be searched for the description 
	 * @param newDescription The description of the NetworkID that should not be present in the list
	 * @return {@code true} id the list contains no NetworkID with a matching description, {@code false} if one was found
	 * @throws NullPointerException If {@code source} or {@code newDescription} are {@code null}
	 */
	public static boolean isUnique(Iterable<NetworkID> source, String newDescription) {
		Objects.requireNonNull(source, "'source' parameter must not be null");
		Objects.requireNonNull(newDescription, "'newDescription' parameter must not be null");
		for(NetworkID id : source) {
			if(id == null) continue; //Skip null entries in the source.
			if(id.getDescription().equals(newDescription)) {
				return false; //The name already exists
			}
		}
		return true; //No id with that name found
	}
	
	/////////////////////////////// FACTORY /////////////////////////////////////////
	
	
	/**
	 * Creates a {@link NetworkID} that implements the {@link NetworkIDFeature#INTERNAL} feature only.
	 * Can be used to identify the local side of a connection, to to connect to internal servers.
	 * @param description The text description of the NetworkID
	 * @return The new NetworkID instance
	 * @throws NullPointerException When the {@code description} is {@code null}
	 */
	public static NetworkID createID(String description) {
		Objects.requireNonNull(description, "'description' parameter must not be null");
		return new LocalNetworkID(description);
	}
	
	/**
	 * Creates a {@link NetworkID} that implements the {@link NetworkIDFeature#NETWORK} and
	 * {@link NetworkIDFeature#BIND} features.
	 * Can be used to give a server a port to bind to.
	 * @param description The text description of the NetworkID
	 * @param port The port to bind to, in range 0 - 65535
	 * @return The new NetworkID instance
	 * @throws IllegalArgumentException When the port number is not in {@code ]0, 65535[}
	 * @throws NullPointerException When the {@code description} is {@code null}
	 */
	public static NetworkID createID(String description, int port) {
		Objects.requireNonNull(description, "'description' parameter must not be null");
		if(port < 0 || port > 65535) throw new IllegalArgumentException("Port must be in range 0 - 65535");
		return new BindNetworkID(description, port);
	}
	
	/**
	 * Creates a {@link NetworkID} that implements the {@link NetworkIDFeature#NETWORK} and
	 * {@link NetworkIDFeature#CONNECT} features.
	 * Can be used to identify a remote endpoint to connect to.
	 * @param description The text description of the NetworkID
	 * @param address The {@link InetSocketAddress} that describes the endpoint to connect to
	 * @return The new NetworkID instance
	 * @throws NullPointerException When any of the parameters are {@code null}
	 */
	public static NetworkID createID(String description, InetSocketAddress address) {
		Objects.requireNonNull(description, "'description' parameter must not be null");
		Objects.requireNonNull(address, "'address' parameter must not be null");
		return new ConnectNetworkID(description, address);
	}
	
	/**
	 * Creates a {@link NetworkID} that implements the {@link NetworkIDFeature#NETWORK} and
	 * {@link NetworkIDFeature#CONNECT} features.
	 * Can be used to identify a remote endpoint to connect to.
	 * @param description The text description of the NetworkID
	 * @param address The {@link InetAddress} that describes the IP-Address/hostname of the endpoint to connect to
	 * @param port The port of the endpoint to connect to, in range 0 - 65535
	 * @return The new NetworkID instance
	 * @throws IllegalArgumentException When the port number is not in {@code ]0, 65535[}
	 * @throws NullPointerException When any of the parameters are {@code null}
	 */
	public static NetworkID createID(String description, InetAddress address, int port) {
		//Don't check description, it is directly passed to createID(String, INSA) and validated there.
		Objects.requireNonNull(address, "'address' parameter must not be null");
		if(port < 0 || port > 65535) throw new IllegalArgumentException("Port must be in range 0 - 65535");
		//Don't check port range either, this is done in the InetSocketAddress constructor
		final InetSocketAddress address2 = new InetSocketAddress(address, port);
		return createID(description, address2);
	}
	
	/**
	 * Creates a {@link NetworkID} that implements the {@link NetworkIDFeature#NETWORK} and
	 * {@link NetworkIDFeature#CONNECT} features.
	 * Can be used to identify a remote endpoint to connect to.
	 * @param description The text description of the NetworkID
	 * @param address A string representation of the IP-Address/hostname of the endpoint to connect to
	 * @param port The port of the endpoint to connect to, in range 0 - 65535
	 * @return A new NetworkID instance
	 * @throws UnknownHostException When the {@code address} could not be resolved with {@link InetAddress#getByName(String)}
	 * @throws IllegalArgumentException When the port number is not in {@code ]0, 65535[}
	 * @throws NullPointerException When any of the parameters are {@code null}
	 */
	public static NetworkID createID(String description, String address, int port) throws UnknownHostException {
		//Don't check description, it is directly passed to createID(String, INSA) and validated there.
		Objects.requireNonNull(address, "'address' parameter must not be null");
		//Don't check port range either, this is done in the InetSocketAddress constructor
		final InetAddress address2 = InetAddress.getByName(address);
		return createID(description, address2, port);
	}
}
