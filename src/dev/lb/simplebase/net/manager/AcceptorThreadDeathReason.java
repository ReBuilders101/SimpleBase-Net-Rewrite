package dev.lb.simplebase.net.manager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

import dev.lb.simplebase.net.connection.TcpSocketNetworkConnection;

/**
 * Contains the reason why a the receiver thread of a {@link TcpSocketNetworkConnection} dies.<br>
 */
public enum AcceptorThreadDeathReason {
	
	/**
	 * The thread died because the underlying {@link ServerSocket} was closed/made unusable by non-API code
	 */
	EXTERNAL,
	/**
	 * The thread died because it was interrupted by calling the {@link Thread#interrupt()} method.<br>
	 * This is considered the only non-exceptional way to end the receiver thread.
	 */
	INTERRUPTED,
	/**
	 * The thread died because the underlying {@link ServerSocket} threw an {@link IOException}.
	 */
	IOEXCEPTION,
	/**
	 * The thread died because the underlying {@link ServerSocket} threw a {@link SocketTimeoutException}.
	 */
	TIMEOUT,
	/**
	 * The thread died for a reason that could not be determined.
	 */
	UNKNOWN;
}