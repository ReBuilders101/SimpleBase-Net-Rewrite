package dev.lb.simplebase.net.events;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

import dev.lb.simplebase.net.config.ConnectionType;
import dev.lb.simplebase.net.event.Event;
import dev.lb.simplebase.net.event.EventAccessor;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.manager.NetworkManagerServer;

/**
 * This event will be fired for the {@link NetworkManagerServer#FilterRawConnection} accessor after a connection is accepted by the server,
 * but before the new {@link Socket} (in case of TCP connections) or newly registered {@link InetSocketAddress} (in case of UDP
 * connections) is processed by the server in any way. This event is the earliest that an incoming connection can be handled.
 * <p>
 * The main purpose of this event is to filter incoming connection based on certain criteria such as the amount of clients already
 * connected to the server, or for an ip-based blacklist system. It can also be used to set the name of the new connection's {@link NetworkID}.
 * </p><p>
 * If the event is cancelled, the connection will be closed as soon as possible and not added to the server.
 * </p><p>
 * The event will not be fired for a connetion of type {@link ConnectionType#INTERNAL}, as these connections are
 * not associated with an {@link InetAddress} and all required objects already exist.
 * </p>
 */
public class FilterRawConnectionEvent extends Event {

	private final InetSocketAddress address;
	private final NetworkManagerServer server;
	private String name;
	
	/**
	 * Constructs a new instance of this event for a {@link NetworkManagerServer} and a connecting {@link InetSocketAddress}.
	 * @param server The {@link NetworkManagerServer} that is accepting the connection and fired this event
	 * @param source The {@link InetSocketAddress} that is trying to connect to the server
	 * @param name The default name that will be chosen for the {@link NetworkID} when no name is configured
	 */
	public FilterRawConnectionEvent(NetworkManagerServer server, InetSocketAddress source, String name) {
		super(true, false);
		this.address = source;
		this.server = server;
		this.name = name;
	}
	
	/**
	 * The {@link NetworkManagerServer} that is accepting the connection and fired this event.
	 * @return The {@link NetworkManagerServer} associated with this event
	 */
	public NetworkManagerServer getServerManager() {
		return server;
	}
	
	/**
	 * The {@link InetSocketAddress} of the client trying to connecet to the server.
	 * @return The {@link InetSocketAddress} of the client
	 */
	public InetSocketAddress getRemoteAddress() {
		return address;
	}
	
	/**
	 * The name that the newly created {@link NetworkID} for this connection will have.
	 * @return The name to use for the {@link NetworkID}
	 */
	public String getNetworkIdName() {
		return name;
	}
	
	/**
	 * Set the name of the {@link NetworkID} that the connection will have (if it is not cancelled).
	 * <p>
	 * The name usually should be unique, but this is not checked by the server in any way. Setting a 
	 * name is not required, and the default names are guaranteed to be unique.</p>
	 * @param name The new name for the {@link NetworkID}
	 * @throws NullPointerException When the {@code name} parameter is {@code null}
	 */
	public void setNetworkIdName(String name) {
		this.name = Objects.requireNonNull(name, "'name' parameter must not be null");
	}

	/**
	 * An implementation of an event handler for the {@link FilterRawConnectionEvent}.
	 * Can be registered for any {@link EventAccessor} with a matching generic type.
	 * <p>
	 * This is only a very basic filter that does not consider more than one connection being
	 * accepted in parallel.
	 * </p>
	 * @param max The maximum amount of players that can connect
	 * @return An event handler that filters connections based on the amount of clients on the server
	 */
	public static final Consumer<FilterRawConnectionEvent> limitClientCount(int max) {
		return (e) -> {
			if(e.server.getClientCount() >= max) e.setCancelled(true);
		};
	}
	
	/**
	 * An implementation of an event handler for the {@link FilterRawConnectionEvent}.
	 * Can be registered for any {@link EventAccessor} with a matching generic type.
	 * <p>
	 * This is only a very basic filter that does not consider concurrent modification
	 * of the {@link Iterable}.
	 * </p>
	 * @param list A list of {@link InetAddress}es that are not allowed to connect to the server 
	 * @return An event handler that filters connections based on their source IP address
	 */
	public static final Consumer<FilterRawConnectionEvent> applyIpBlacklist(Iterable<InetAddress> list) {
		return (e) -> {
			for(InetAddress i : list) {
				if(e.address.getAddress().equals(i)) {
					e.setCancelled(true);
					break;
				}
			}
		};
	}
	
}
