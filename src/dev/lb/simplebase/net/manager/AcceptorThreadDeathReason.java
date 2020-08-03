package dev.lb.simplebase.net.manager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

/**
 * Contains options for reasons why a {@link ServerSocketAcceptorThread} ends.<br>
 * Can be passed to the {@link NetworkManagerServer} that handled the {@link ServerSocket}.
 */
public enum AcceptorThreadDeathReason {
	/**
	 * The thread endend because the underlying {@link ServerSocket} was closed/made unusable by non-API code
	 */
	EXTERNAL,
	/**
	 * The thread ended because it was interrupted by calling the {@link Thread#interrupt()} method.<br>
	 * This is considered the only non-exceptional way to end this thread.
	 */
	INTERRUPTED,
	/**
	 * The thread endend because the underlying {@link ServerSocket} threw an {@link IOException}.
	 */
	IOEXCEPTION,
	/**
	 * The thread endend because the underlying {@link ServerSocket} threw a {@link SocketTimeoutException}.
	 */
	TIMEOUT,
	/**
	 * The thread endend for a reason that could not be determined.
	 */
	UNKNOWN;
}