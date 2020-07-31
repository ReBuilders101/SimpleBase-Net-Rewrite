package dev.lb.simplebase.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.connection.NetworkConnectionState;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;

public class TCPSocketNetworkConnection extends NetworkConnection {

	private final DataReceiverThread dataReceiverThread;
	private final Socket socket;
	private final Object lockSocketWrite;
	private final PacketConverter converter;

	protected TCPSocketNetworkConnection(NetworkID localID, NetworkID remoteID, NetworkManagerCommon networkManager,
			int checkTimeoutMS, boolean serverSide, Object customObject, Supplier<Socket> socketFactory) {
		super(localID, remoteID, networkManager, NetworkConnectionState.INITIALIZED, checkTimeoutMS, serverSide, customObject);

		this.socket = socketFactory.get();
		this.dataReceiverThread = new DataReceiverThread();
		this.converter = new PacketConverter(networkManager, this);
		this.lockSocketWrite = new Object();
	}

	protected TCPSocketNetworkConnection(NetworkID localID, NetworkID remoteID, NetworkManagerCommon networkManager,
			int checkTimeoutMS, boolean serverSide, Object customObject, Socket serverSideSocket) {
		super(localID, remoteID, networkManager, NetworkConnectionState.assertOpen(serverSideSocket), checkTimeoutMS, serverSide, customObject);

		this.socket = serverSideSocket;
		this.dataReceiverThread = new DataReceiverThread();
		this.dataReceiverThread.start();
		this.converter = new PacketConverter(networkManager, this);
		this.lockSocketWrite = new Object();
	}

	private void writeToSocket(byte[] data) {
		try {
			synchronized (lockSocketWrite) {
				socket.getOutputStream().write(data);
			}
		} catch (IOException e) {
			NetworkManager.NET_LOG.error("Exception while writing to connection stream:");
			NetworkManager.NET_LOG.error(e);
		}
	}

	@Override
	protected void openConnectionImpl() {
		currentState = NetworkConnectionState.OPENING;
		try {
			socket.connect(getRemoteID().getFunction(NetworkIDFunction.CONNECT));
			dataReceiverThread.start();
			currentState = NetworkConnectionState.OPEN;
		} catch (IOException e) {
			NetworkManager.NET_LOG.error("Error while opening TCPSocketNetworkConnection. Connection closed");
			NetworkManager.NET_LOG.error(e);
			currentState = NetworkConnectionState.CLOSED;
		}
	}

	@Override
	protected void closeConnectionImpl(ConnectionCloseReason reason) {
		closeConnectionImpl(reason, null);
	}

	protected void closeConnectionImpl(ConnectionCloseReason reason, Exception exception) {
		try {
			currentState = NetworkConnectionState.CLOSING;
			NetworkManager.NET_LOG.info("Closing connection: %s; Reason: %s", getClass().getCanonicalName(), reason);
			postEventAndRemoveConnection(reason, null);
			dataReceiverThread.quitSilently = true; //ends the receiver thread without re-calling closeImpl
			socket.close();
		} catch (IOException e) {
			NetworkManager.NET_LOG.error("Exception while closing connection:");
			NetworkManager.NET_LOG.error(e);
		} finally {
			currentState = NetworkConnectionState.CLOSED;
		}
	}

	@Override
	protected void checkConnectionImpl(int uuid) {
		final byte[] toSend = converter.checkToArray(uuid);
		writeToSocket(toSend);
	}

	@Override
	protected void sendPacketImpl(Packet packet) {
		final byte[] toSend = converter.packetToArray(packet);
		writeToSocket(toSend);
	}

	@Override
	protected void receiveConnectionCheck(int uuid) {
		final byte[] toSend = converter.checkReplyToArray(uuid);
		writeToSocket(toSend);
	}

	@Override
	protected void closeTimeoutImpl() {
		synchronized (lockCurrentState) {
			closeConnectionImpl(ConnectionCloseReason.TIMEOUT);
		}
	}

	@Override
	protected void receiveUDPLogout() {
		synchronized (lockCurrentState) {
			NetworkManager.NET_LOG.warning("TCP connections should never receive UDP logout requests");
			closeConnectionImpl(ConnectionCloseReason.REMOTE);
		}
	}

	private static final AtomicInteger DATA_THREAD_ID = new AtomicInteger(0);
	private class DataReceiverThread extends Thread {

		//If this is true, the thread will close before the next loop cycle and will never notify the connection of the thread death
		private volatile boolean quitSilently = false;
		private final InputStream stream;
		
		protected DataReceiverThread() {
			super("TCPSocketNetworkConnection-DataReceiverThread-" + DATA_THREAD_ID.getAndIncrement());
			try {
				stream = TCPSocketNetworkConnection.this.socket.getInputStream();
			} catch (IOException e) {
				throw new IllegalStateException(e); //Should not happen
			}
		}

		@Override
		public void run() {
			while(true) {
				if(Thread.interrupted()) {
					Thread.currentThread().interrupt();
					tryNotifyClose(ConnectionCloseReason.INTERRUPTED, null);
				}
				
				if(quitSilently) { //Just quit, don't do anything
					NetworkManager.NET_LOG.debug("Data receiver thread quit silently");
					return;
				}
				
				try {
					int value = stream.read();
					if(value == -1) {
						tryNotifyClose(ConnectionCloseReason.REMOTE, null);
					} else {
						byte data = (byte) (value & 0xFF);
						converter.receiveByte(data);
					}
				} catch (SocketException e) {
					tryNotifyClose(ConnectionCloseReason.EXTERNAL, e);
				} catch (IOException e) {
					tryNotifyClose(ConnectionCloseReason.IOEXCEPTION, e);
				}
				
			}
		}

		private void tryNotifyClose(ConnectionCloseReason reason, Exception exception) {
			if(!quitSilently) { //If the thread is set to quit silently, don't notify anyone (except a log message)
				TCPSocketNetworkConnection.this.closeConnectionImpl(reason, exception);
			} else {
				NetworkManager.NET_LOG.debug("Data receiver thread quit silently");
			}
		}
		
	}

}
