package dev.lb.simplebase.net.events;

import java.io.IOException;
import java.net.Socket;

public enum ConnectionCloseReason {
	/**
	 * The connection was closed because the close method was called on the {@link AbstractNetworkConnection} on the local side
	 */
	EXPECTED,
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
	 * Timed out after a check request
	 */
	TIMEOUT;
}
