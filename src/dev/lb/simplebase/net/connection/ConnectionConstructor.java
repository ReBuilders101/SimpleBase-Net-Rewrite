package dev.lb.simplebase.net.connection;

import java.io.IOException;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.packet.PacketContext;

/**
 * A {@link FunctionalInterface} that represents a constrructor of an unknown implementation of {@link NetworkConnection}.
 */
@Internal
@FunctionalInterface
public interface ConnectionConstructor {

	/**
	 * Construct a new instance of an implementation of {@link NetworkConnection}.
	 * <p>
	 * The parameters of this method do not include all parameters necessary to construct any {@link NetworkConnection}
	 * (e.g. the local {@link NetworkID}). Those values are usually known in the place that this functional interface
	 * is <i>implemented</i> (usually as a lambda expression) and can be captured by the closure.
	 * </p>
	 * <h2>Example</h2><p>
	 * Constructor of {@code ExampleNetworkConnection} (not a real class):
	 * <pre>
	 * public ExampleNetworkConnection(NetworkID local, NetworkID remote, int someExtraData, Object customData) {
	 * 	//Store values and call super constructor
	 * }
	 * </pre>
	 * Creating a {@code ConnectionConstructor} by capturing known values
	 * <pre>
	 * //manager is the NetworkManagerServer that will hold the connection after creation
	 * public void foo() {
	 * 	//remote and data are supplied to the functional interface later, but the local ID and the extra
	 *	//data are known right now and can be included in the closure now
	 * 	final ConnectionConstructor ctor = (remote, data) -> new ExampleNetworkConnection(
	 * 		manager.getLocalId(), remote, this.extraData(), data);
	 * }
	 * </pre>
	 * </p>
	 * @param id The {@link NetworkID} representing the remote side of the connection.
	 * @param customData The custom data object that will be associated with the connection's {@link PacketContext}
	 * @return The created {@link NetworkConnection}
	 * @throws IOException When the {@link NetworkConnection} constructor fails for any reason
	 */
	public NetworkConnection construct(NetworkID id, Object customData) throws IOException;
	
}
