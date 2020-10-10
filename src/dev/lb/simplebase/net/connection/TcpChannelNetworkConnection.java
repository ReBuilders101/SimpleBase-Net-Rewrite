package dev.lb.simplebase.net.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFeature;
import dev.lb.simplebase.net.manager.ExternalNetworkManagerServer;
import dev.lb.simplebase.net.manager.NetworkManagerServer;
import dev.lb.simplebase.net.manager.SelectorManager;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.task.Task;
import dev.lb.simplebase.net.util.InternalAccess;

/**
 * <h2>Internal use only</h2>
 * <p>
 * This class is used internally by the API and the contained methods should not and can not be called directly.
 * </p><hr><p>
 * A {@link NetworkConnection} implementation using a non-blocking {@link SocketChannel}. This connection type
 * is only used on the server side, and the {@link NetworkManagerServer} provides the {@link Selector} used for
 * all connections of this type.
 * </p>
 */
@Internal
public final class TcpChannelNetworkConnection extends ExternalNetworkConnection implements ChannelConnection {
	
	private final SocketChannel channel;
	private SelectionKey selectionKey; //Not final - only set while holding lock
	private final SelectorManager selectorManager;
	private final ByteBuffer receiveBuffer;
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This constructor is used internally by the API and should not and can not be called directly.
	 * </p><hr><p>
	 * Creates a new server-side connection implementation using a {@link SocketChannel}.
	 * </p>
	 * @param networkManager The server manager used by this connection
	 * @param selctorManager The object that provides a {@link Selector}. Usually the same as the server manager
	 * @param remoteID The {@link NetworkID} of the remote side of the connection
	 * @param channel The {@link SocketChannel} to use for this connection. Will be set to non-blocking mode
	 * @param customObject The costom data for the connection's {@link PacketContext}
	 * @throws IOException When changing the channel to non-blocking mode throws an exception
	 */
	public TcpChannelNetworkConnection(NetworkManagerServer networkManager, SelectorManager selctorManager, NetworkID remoteID,
			SocketChannel channel, Object customObject) throws IOException {
		super(networkManager, remoteID, NetworkConnectionState.OPEN,
				networkManager.getConfig().getConnectionCheckTimeout(), false, customObject, true);
		InternalAccess.assertCaller(ExternalNetworkManagerServer.class, 1, "Cannot instantiate TcpChannelConnection directly");
		
		this.channel = channel;
		this.channel.configureBlocking(false);
		this.selectorManager = selctorManager;
		this.receiveBuffer = ByteBuffer.allocate(networkManager.getConfig().getPacketBufferInitialSize());
		this.selectionKey = selctorManager.registerConnection(channel, SelectionKey.OP_READ, this);
	}
	
	@Override
	protected void sendRawByteData(ByteBuffer buffer) {
		if(channel.socket().isClosed()) {
			SEND_LOGGER.warning("Cannot send data: Socket already closed");
		} else {
			synchronized (lockCurrentState) { //Sync at the very end of packet processing, so that the monitor is free while encoding 
				if(channel.socket().isClosed()) {
					SEND_LOGGER.warning("Cannot send data: Socket already closed");
				} else {
					try {
						channel.write(buffer);
					} catch (IOException e) {
						SEND_LOGGER.error("Cannot send data: Socket throws exception", e);
					}
				}
			}
		}
	}

	@Override
	protected Task openConnectionImpl() {
		try {
			channel.connect(remoteID.getFeature(NetworkIDFeature.CONNECT));
			selectionKey = selectorManager.registerConnection(channel, SelectionKey.OP_CONNECT, this);
			if(selectionKey == null) {
				STATE_LOGGER.error("Cannot open channel connection: No selection key");
				try {
					channel.close();
				} catch (IOException e) { //Catch and log this separately
					STATE_LOGGER.error("Cannot close socket channel (error while opening)", e);
				}
				currentState = NetworkConnectionState.CLOSED;
				return Task.completed();
			} else {
				return openCompleted;
			}
		} catch (IOException e) {
			STATE_LOGGER.error("Cannot connect socket channel to server", e);
			currentState = NetworkConnectionState.CLOSED;
			return Task.completed();
		}
	}

	@Override
	protected Task closeConnectionImpl(ConnectionCloseReason reason) {
		openCompleted.release(); //Don't wait for open when the connection is closed
		if(selectionKey != null) selectionKey.cancel();
		try {
			channel.close();
		} catch (IOException e) {
			STATE_LOGGER.error("Cannot close socket channel", e);
		}
		postEventAndRemoveConnection(reason, null);
		currentState = NetworkConnectionState.CLOSED;
		return Task.completed();
	}

	@Override
	public void readNow() {
		receiveBuffer.clear();
		synchronized (currentState) {
			if(currentState.canSendData()) { //SEND and READ require the same states
				try {
					channel.read(receiveBuffer);
				} catch (IOException e) {
					RECEIVE_LOGGER.error("Error while reading from socket channel", e);
					return;
				}
			} else {
				RECEIVE_LOGGER.warning("Cannot read data: Channel was closed");
			}
		}
		receiveBuffer.flip();
		byteAccumulator.acceptBytes(receiveBuffer);
	}
	
}
