package dev.lb.simplebase.net.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;

public class TcpSocketNetworkConnection extends ConvertingNetworkConnection {

	private final DataReceiverThread thread;
	private final Socket socket;
	
	public TcpSocketNetworkConnection(NetworkManagerCommon networkManager, NetworkID remoteID,
			int checkTimeoutMS, boolean serverSide, Object customObject) {
		super(networkManager, remoteID, NetworkConnectionState.INITIALIZED, checkTimeoutMS, serverSide, customObject, true);
		this.thread = new DataReceiverThread();
		this.socket = new Socket();
	}

	@Override
	protected NetworkConnectionState openConnectionImpl() {
		try {
			socket.connect(remoteID.getFunction(NetworkIDFunction.CONNECT));
			thread.start();
			return NetworkConnectionState.OPEN;
		} catch (IOException e) {
			STATE_LOGGER.error("Cannot connect socket to server", e);
			return NetworkConnectionState.CLOSED;
		}
	}

	@Override
	protected NetworkConnectionState closeConnectionImpl(ConnectionCloseReason reason) {
		thread.interrupt(); //Will close the socket
		postEventAndRemoveConnection(reason, null);
		return NetworkConnectionState.CLOSED;
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
						socket.getOutputStream().write(buffer.array());
					} catch (IOException e) {
						SEND_LOGGER.error("Cannot send data: Socket throws exception", e);
					}
				}
			}
		}
	}

	private class DataReceiverThread extends Thread {

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
						byteToPacketConverter.acceptByte((byte) (data & 0xFF));
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
