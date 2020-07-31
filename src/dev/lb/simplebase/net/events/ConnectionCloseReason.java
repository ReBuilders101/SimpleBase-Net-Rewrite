package dev.lb.simplebase.net.events;

import java.io.IOException;
import java.net.Socket;

import dev.lb.simplebase.net.NetworkManagerServer;
import dev.lb.simplebase.net.connection.NetworkConnectionState;

/**
 * Gives a reason why a connection was closed in the {@link ConnectionClosedEvent}
 */
public enum ConnectionCloseReason {
	/**
	 * The connection was closed because the close method was called on the {@link AbstractNetworkConnection} on the local side
	 */
	EXPECTED,
	/**
	 * The connection was closed because it was closed when the {@link NetworkManagerServer} was stopped
	 */
	SERVER,
	/**
	 * The connection was closed because the thread that receives and processes incoming data from was interrupted and stopped 
	 */
	INTERRUPTED,
	/**
	 * The connection was closed because the remote side of the connection was closed
	 */
	REMOTE,
	/**
	 * The connection was closed because an underlying object, e.g. a {@link Socket}, was closed by non-API code
	 */
	EXTERNAL,
	/**
	 * The connection was closed because an {@link IOException} was thrown when interacting with an underlying object, e.g. a {@link Socket}
	 */
	IOEXCEPTION,
	/**
	 * The connection was closed for an unknown reason
	 */
	UNKNOWN,
	/**
	 * The connection was closed beacuse the timeout expired in {@link NetworkConnectionState#CHECKING} 
	 * and this was detected by the global timeout check thread.
	 */
	TIMEOUT;
}
