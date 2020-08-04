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
			LOGGER.info("TCP ServerSocket listener thread started");
			while(!this.isInterrupted()) {
				try {
					final Socket socket = serverSocket.accept();
					manager.acceptIncomingRawTcpConnection(socket);
				} catch (SocketException e) {
					if(this.isInterrupted()) {
						deathReason = AcceptorThreadDeathReason.INTERRUPTED;
					} else {
						deathReason = AcceptorThreadDeathReason.EXTERNAL;
						LOGGER.error("TCP ServerSocket listener thread: Unexpected error", e);
					}
					return;
				} catch (SocketTimeoutException e) {
					deathReason = AcceptorThreadDeathReason.TIMEOUT;
					LOGGER.error("TCP ServerSocket listener thread: Unexpected error", e);
					return;
				} catch (IOException e) {
					deathReason = AcceptorThreadDeathReason.IOEXCEPTION;
					LOGGER.error("TCP ServerSocket listener thread: Unexpected error", e);
					return;
				}
			}
		} finally {
			//Always run these, however the thread ends
			manager.notifyTCPAcceptorThreadClosure(deathReason);
			LOGGER.info("TCP ServerSocket listener thread ended with reason %s", deathReason);
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
}
