package dev.lb.simplebase.net.events;

import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.event.Event;
import dev.lb.simplebase.net.event.EventDispatcher;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.manager.NetworkManagerServer;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.packet.handler.PacketHandler;

/**
 * This event will be fired for the {@link NetworkManagerServer#ConfigureConnection} accessor after a connection has
 * been accepted by the server, but before the {@link NetworkConnection} object for this connection is constructed.
 * <p>
 * The main purpose of this event is to attach a custom object to the new connection's {@link PacketContext} that
 * is available in all {@link PacketHandler}s.
 * </p>
 */
public class ConfigureConnectionEvent extends Event {

	/**
	 * Can be used with {@link #setCustomObject(Object)} to attach no custom object
	 * or remove the currently attached object.
	 */
	public static final Object NO_CUSTOM_DATA = null;
	
	private final NetworkID remoteId;
	private final NetworkManagerServer server;
	
	private Object customObject;
	
	/**
	 * Cunstructs a new instance of this event for a {@link NetworkManagerServer} and a {@link NetworkID}
	 * idetifying the client for which the connection is being configured.
	 * @param server The {@link NetworkManagerServer} that accepts the connection
	 * @param remoteId The {@link NetworkID} identifying the client that is connecting
	 */
	public ConfigureConnectionEvent(NetworkManagerServer server, NetworkID remoteId) {
		super(false, false);
		this.server = server;
		this.remoteId = remoteId;
		this.customObject = null;
	}

	/**
	 * The server that is accepting the connection. The event will be posted through this
	 * manager's {@link EventDispatcher}.
	 * @return The server for which the event was posted
	 */
	public NetworkManagerServer getServer() {
		return server;
	}
	
	/**
	 * The {@link NetworkID} of the server manager. Identical to calling
	 * {@link NetworkManagerServer#getLocalID()} on the server returned by {@link #getServer()}.
	 * @return The local ID of the associated server
	 */
	public NetworkID getLocalId() {
		return server.getLocalID();
	}
	
	/**
	 * The {@link NetworkID} of the client that is connecting to the server
	 * @return The {@link NetworkID} of the client
	 */
	public NetworkID getRemoteId() {
		return remoteId;
	}
	
	/**
	 * The custom object that is currently attached to this event.<p>
	 * This method will be called after event handling is done to get the object that
	 * should be associated with the {@link PacketContext}.</p>
	 * @return The current custom object
	 */
	public Object getCustomObject() {
		return customObject;
	}
	
	/**
	 * Sets the custom object that will be associated with the connection's {@link PacketContext} after
	 * all event handlers have run.<p>
	 * When intending to not attach any object or to remove an object attached by a previous handler
	 * it is recommended to use the constant {@link #NO_CUSTOM_DATA} instead of {@code null}.
	 * </p><p>
	 * The attached object might be overwritten by a handler that runs after this one (with a lower priority).
	 * </p>
	 * @param data The object to attach to the event (and to the connection)
	 */
	public void setCustomObject(Object data) {
		this.customObject = data;
	}
	
}
