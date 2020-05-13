package dev.lb.simplebase.net.id;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.function.Function;

import dev.lb.simplebase.net.annotation.Immutable;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.annotation.ValueType;

/**
 * An Instance of NetworkID represents a network party and contains all necessary information to make a connection.
 * Instances can be created through static methods in this class.
 */
@ValueType
@Threadsafe
@Immutable
public abstract class NetworkID implements Cloneable {

	protected final String description;
	
	protected NetworkID(String description) {
		this.description = description;
	}
	
	/**
	 * A text description/identifier for this NetworkID. Usually used as an unique identifier, but this
	 * behavior must be guaranteed by the object that stores the NetworkIDs.
	 * @return The description text for this NetworkID
	 */
	public final String getDescription() {
		return description;
	}
	
	/**
	 * Checks whether this NetworkID implements the requested optional functionality.
	 * No NetworkID can implement the {@code null} function, so this method always returns {@code false}
	 * if the parameter has the value {@code null}.
	 * @param function The optional functionality that this NetworkID might implement
	 * @return Whether this NetworkID implements the functionality.
	 */
	public abstract boolean hasFunction(NetworkIDFunction function);
	
	/**
	 * An alternate {@link #toString()} implementation. While the normal {@code toString} method lists all member values
	 * and the implementation type for debugging, this method can be used to generate more human-readable descriptions.
	 * It will always contain the NetworkID's description string, and if the parameter {@code includeDetails} is true,
	 * optional networking information.
	 * @param includeDetails Whether the network details beside the description are included in the string
	 * @return A configurable String representation of this {@link NetworkID}
	 */
	public abstract String toString(boolean includeDetails);
	
	/**
	 * All NetworkID implementations also implement the {@link Cloneable} interface and provide a public clone method.
	 * @return A new instance of NetworkID with the same implementation class and the same member values
	 */
	@Override
	public abstract NetworkID clone();
	
	/**
	 * It is often desired that the description of a NetworkID uniquely identifies the instance.
	 * This makes the normal {@link #clone()} method basically useless, since the description will be copied as well.<br>
	 * This method can change the description while keeping the other members and the implementing class the same.
	 * @param newDescription The new description for the cloned NetworkID instance.
	 * @return A new NetworkID with identical member values except for the description
	 */
	public abstract NetworkID clone(String newDescription);
	
	/**
	 * It is often desired that the description of a NetworkID uniquely identifies the instance.
	 * This makes the normal {@link #clone()} method basically useless, since the description will be copied as well.<br>
	 * This method can change the description while keeping the other members and the implementing class the same.
	 * In some cases the new description will directly depend on the old one,
	 * and this method can use a mapping function to generate the new description.
	 * @param newDescription A function that maps the old description to the new one. The string passed to the mapping function will never be {@code null}.
	 * @return A new NetworkID with identical member values except for the description
	 * @throws NullPointerException if the mapping function is {@code null} or returns {@code null}.
	 */
	public NetworkID clone(Function<String, String> deriveDescription) {
		Objects.requireNonNull(deriveDescription, "'derivedDescription' parameter must not be null");
		return clone(deriveDescription.apply(description));
	}
	
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
	 * Creates a {@link NetworkID} that implements the {@link NetworkIDFunction#LOCAL} functionality only.
	 * Can be used to identify the local side of a connection, to to connect to internal servers
	 * @param description The text description of the NetworkID
	 * @return A new NetworkID instance
	 */
	public static NetworkID createID(String description) {
		Objects.requireNonNull(description, "'description' parameter must not be null");
		return new LocalNetworkID(description);
	}
	
	/**
	 * Creates a {@link NetworkID} that implements the {@link NetworkIDFunction#NETWORK} and
	 * {@link NetworkIDFunction#BIND} functionalities.
	 * Can be used to give a server a port to bind to.
	 * @param description The text description of the NetworkID
	 * @param port The port to bind to, in range 0 - 65535
	 * @return A new NetworkID instance
	 */
	public static NetworkID createID(String description, int port) {
		Objects.requireNonNull(description, "'description' parameter must not be null");
		if(port < 0 || port > 65535) throw new IllegalArgumentException("Port must be in range 0 - 65535");
		return new BindNetworkID(description, port);
	}
	
	/**
	 * Creates a {@link NetworkID} that implements the {@link NetworkIDFunction#NETWORK} and
	 * {@link NetworkIDFunction#CONNECT} functionalities.
	 * Can be used to identify a remote endpoint to connect to.
	 * @param description The text description of the NetworkID
	 * @param address The {@link InetSocketAddress} that describes the endpoint to connect to
	 * @return A new NetworkID instance
	 */
	public static NetworkID createID(String description, InetSocketAddress address) {
		Objects.requireNonNull(description, "'description' parameter must not be null");
		Objects.requireNonNull(address, "'address' parameter must not be null");
		return new ConnectNetworkID(description, address);
	}
	
	/**
	 * Creates a {@link NetworkID} that implements the {@link NetworkIDFunction#NETWORK} and
	 * {@link NetworkIDFunction#CONNECT} functionalities.
	 * Can be used to identify a remote endpoint to connect to.
	 * @param description The text description of the NetworkID
	 * @param address The {@link InetAddress} that describes the IP-Address/hostname of the endpoint to connect to
	 * @param port The port of the endpoint to connect to, in range 0 - 65535
	 * @return A new NetworkID instance
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
	 * Creates a {@link NetworkID} that implements the {@link NetworkIDFunction#NETWORK} and
	 * {@link NetworkIDFunction#CONNECT} functionalities.
	 * Can be used to identify a remote endpoint to connect to.
	 * @param description The text description of the NetworkID
	 * @param address A string representation of the IP-Address/hostname of the endpoint to connect to
	 * @param port The port of the endpoint to connect to, in range 0 - 65535
	 * @return A new NetworkID instance
	 */
	public static NetworkID createID(String description, String address, int port) throws UnknownHostException {
		//Don't check description, it is directly passed to createID(String, INSA) and validated there.
		Objects.requireNonNull(address, "'address' parameter must not be null");
		//Don't check port range either, this is done in the InetSocketAddress constructor
		final InetAddress address2 = InetAddress.getByName(address);
		return createID(description, address2, port);
	}
}
