package dev.lb.simplebase.net.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.ExternalNetworkManagerServer;
import dev.lb.simplebase.net.manager.NetworkManagerClient;
import dev.lb.simplebase.net.manager.NetworkManagerServer;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.task.Task;
import dev.lb.simplebase.net.util.InternalAccess;

/**
 * <h2>Internal use only</h2>
 * <p>
 * This class is used internally by the API and the contained methods should not and can not be called directly.
 * </p><hr><p>
 * A {@link NetworkConnection} implementation using a {@link Socket}. Can be used on client and server side.
 * </p>
 */
@Internal
public class TcpSocketNetworkConnection extends ExternalNetworkConnection {

	private final DataReceiverThread thread;
	private final Socket socket;
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This constructor is used internally by the API and should not and can not be called directly.
	 * </p><hr><p>
	 * Create a new client-side connection implementation using a {@link Socket}.
	 * </p>
	 * @param networkManager The client manager used by this connection
	 * @param remoteID The {@link NetworkID} of the remote side of the connection
	 * @param customObject The costom data for the connection's {@link PacketContext}
	 */
	public TcpSocketNetworkConnection(NetworkManagerClient networkManager, NetworkID remoteID, Object customObject) {
		super(networkManager, remoteID, NetworkConnectionState.INITIALIZED,
				networkManager.getConfig().getConnectionCheckTimeout(), false, customObject, true);
		InternalAccess.assertCaller(NetworkManagerClient.class, 0, "Cannot instantiate TcpSocketNetworkConnection directly");
		this.socket = new Socket();
		this.thread = new DataReceiverThread();
	}
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This constructor is used internally by the API and should not and can not be called directly.
	 * </p><hr><p>
	 * Create a new server-side connection implementation using a {@link Socket}.
	 * </p>
	 * @param networkManager The server manager used by this connection
	 * @param remoteID The {@link NetworkID} of the remote side of the connection
	 * @param activeSocket The socket created from the {@link ServerSocket} for this connection
	 * @param customObject The costom data for the connection's {@link PacketContext}
	 * @throws IOException When the supplied socket is in an incorrect state
	 */
	public TcpSocketNetworkConnection(NetworkManagerServer networkManager, NetworkID remoteID, Socket activeSocket,
			Object customObject) throws IOException {
		super(networkManager, remoteID, assertSocketState(activeSocket), 
				networkManager.getConfig().getConnectionCheckTimeout(), true, customObject, true);
		InternalAccess.assertCaller(ExternalNetworkManagerServer.class, 1, "Cannot instantiate TcpSocketNetworkConnection directly");
		this.socket = activeSocket;
		this.thread = new DataReceiverThread();
		this.thread.start();
	}

	private static NetworkConnectionState assertSocketState(Socket socket) throws IOException {
		if(socket.isClosed()) throw new IOException("Socket is already closed");
		if(!socket.isConnected()) throw new IOException("Socket is not connected");
		return NetworkConnectionState.OPEN;
	}
	
	@Override
	protected Task openConnectionImpl() {
		try {
			socket.connect(remoteID.getFunction(NetworkIDFunction.CONNECT));
			thread.start();
			return openCompleted;
		} catch (IOException e) {
			STATE_LOGGER.error("Cannot connect socket to server", e);
			currentState = NetworkConnectionState.CLOSED;
			return Task.completed();
		}
	}

	@Override
	protected Task closeConnectionImpl(ConnectionCloseReason reason) {
		openCompleted.release(); //Don't wait for open when the connection is closed
		thread.interrupt(); //Will close the socket
		postEventAndRemoveConnection(reason, null);
		currentState = NetworkConnectionState.CLOSED;
		return Task.completed();
	}
	
	@Override
	protected void sendRawByteData(ByteBuffer buffer) {
		if(socket.isClosed()) {
			SEND_LOGGER.warning("Cannot send data: Socket already closed");
		} else {
			synchronized (lockCurrentState) { //Sync at the very end of packet processing, so that the monitor is free while encoding 
				if(socket.isClosed()) {
					SEND_LOGGER.warning("Cannot send data: Socket already closed");
				} else {
					try {
						final byte[] array = new byte[buffer.remaining()];
						buffer.get(array);
						socket.getOutputStream().write(array);
					} catch (IOException e) {
						SEND_LOGGER.error("Cannot send data: Socket throws exception", e);
					}
				}
			}
		}
	}

	private static final AtomicInteger THREAD_ID = new AtomicInteger(0);
	private class DataReceiverThread extends Thread {
		
		public DataReceiverThread() {
			super("DataReceiverThread-" + THREAD_ID.getAndIncrement());
		}
		
		@Override
		public void run() {
			ConnectionCloseReason closeReason = ConnectionCloseReason.UNKNOWN;
			try {
				RECEIVE_LOGGER.info("Socket receiver thread started");
				while(!this.isInterrupted()) {
					final InputStream stream;
					try {
						stream = socket.getInputStream();
					} catch (IOException e) { //Someone else messed with our socket
						closeReason = ConnectionCloseReason.EXTERNAL;
						return;
					}

					try {
						int data = stream.read();
						if(data == -1) { //Stream shut down
							closeReason = ConnectionCloseReason.REMOTE;
							return;
						}
						byteAccumulator.acceptByte((byte) (data & 0xFF));
					} catch (IOException e) {
						if(isInterrupted()) { //Blocking read call was ended by this.interrupt()
							closeReason = ConnectionCloseReason.INTERRUPTED;
						} else {
							RECEIVE_LOGGER.error("Socket receiver thread: Unexpected error", e);
							closeReason = ConnectionCloseReason.IOEXCEPTION;
						}
						return;
					}
				}
			} finally {
				closeConnection(closeReason);
				RECEIVE_LOGGER.info("Socket listener thread ended with reason %s", closeReason);
			}
		}

		@Override
		public void interrupt() {
			super.interrupt();
			if(isInterrupted() && !socket.isClosed()) {
				try {
					socket.close();
				} catch (IOException e) {
					RECEIVE_LOGGER.error("Cannot close the socket for receiver thread interrupt");
				}
			}
		}

	}

}
