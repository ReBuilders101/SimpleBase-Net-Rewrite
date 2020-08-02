package dev.lb.simplebase.net.manager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.log.AbstractLogger;

/**
 * Listens for incoming socket connections for a server.<p>
 * End the thread by interrupting it ({@link #interrupt()}).
 */
public class ServerSocketAcceptorThread extends Thread {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("server-accept");
	private static final AtomicInteger THREAD_ID = new AtomicInteger(0);

	private final ServerSocket serverSocket;
	private final SocketNetworkManagerServer manager;

	public ServerSocketAcceptorThread(SocketNetworkManagerServer server, ServerSocket socket) {
		super("ServerSocketAcceptorThread-" + THREAD_ID.getAndIncrement());
		this.serverSocket = socket;
		this.manager = server;
	}

	@Override
	public void run() {
		AcceptorThreadDeathReason deathReason = AcceptorThreadDeathReason.UNKNOWN;
		try {
			LOGGER.info("ServerSocket listener thread started");
			while(!this.isInterrupted()) {
				try {
					final Socket socket = serverSocket.accept();
					manager.acceptIncomingRawConnection(socket);
				} catch (SocketException e) {
					if(this.isInterrupted()) {
						deathReason = AcceptorThreadDeathReason.INTERRUPTED;
					} else {
						deathReason = AcceptorThreadDeathReason.EXTERNAL;
						LOGGER.error("ServerSocket listener thread: Unexpected error", e);
					}
					return;
				} catch (SocketTimeoutException e) {
					deathReason = AcceptorThreadDeathReason.TIMEOUT;
					LOGGER.error("ServerSocket listener thread: Unexpected error", e);
					return;
				} catch (IOException e) {
					deathReason = AcceptorThreadDeathReason.IOEXCEPTION;
					LOGGER.error("ServerSocket listener thread: Unexpected error", e);
					return;
				}
			}
		} finally {
			//Always run these, however the thread ends
			manager.notifyAcceptorThreadClosure(deathReason);
			LOGGER.info("ServerSocket listener thread ended with reason %s", deathReason);
		}
	}

	@Override
	public void interrupt() {
		super.interrupt();
		//This will produce a SocketException in the accept() method, ending the loop
		if(isInterrupted() && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				LOGGER.error("Cannot close the server socket for listener thread interrupt");
			}
		}
	}

	/**
	 * Contains options for reasons why a {@link ServerSocketAcceptorThread} ends.<br>
	 * Can be passed to the {@link NetworkManagerServer} that handled the {@link ServerSocket}.
	 */
	public static enum AcceptorThreadDeathReason {
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
}
