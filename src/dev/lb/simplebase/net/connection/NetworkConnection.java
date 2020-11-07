package dev.lb.simplebase.net.connection;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.events.ConnectionClosedEvent;
import dev.lb.simplebase.net.events.PacketSendingFailedEvent;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.Logger;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.manager.NetworkManagerServer;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.task.AwaitableTask;
import dev.lb.simplebase.net.task.Task;
import dev.lb.simplebase.net.util.ThreadsafeAction;

/**
 * A {@link NetworkConnection} represents one side of a client-server connection.
 */
@Threadsafe
public abstract class NetworkConnection {
	protected static final Logger RECEIVE_LOGGER = NetworkManager.getModuleLogger("connection-receive");
	protected static final Logger SEND_LOGGER = NetworkManager.getModuleLogger("connection-send");
	protected static final Logger STATE_LOGGER = NetworkManager.getModuleLogger("connection-state");
	
	//Sync/state
	protected volatile NetworkConnectionState currentState;
	protected final Object lockCurrentState;
	//Components
	protected final PingTracker pingTracker;
	protected final PacketContext packetContext;
	protected final Threadsafe threadsafeState;
	//Values
	protected final NetworkManagerCommon networkManager;
	protected final NetworkID remoteID;
	private final boolean isServerSide;
	
	protected NetworkConnection(NetworkManagerCommon networkManager, NetworkID remoteID,
			NetworkConnectionState initialState, int checkTimeoutMS, boolean serverSide, Object customObject) {
		Objects.requireNonNull(remoteID, "'remoteID' parameter must not be null");
		Objects.requireNonNull(networkManager, "'networkManager' parameter must not be null");
		Objects.requireNonNull(initialState, "'initialState' parameter must not be null");
		
		this.networkManager = networkManager;
		this.remoteID = remoteID;
		this.isServerSide = serverSide;
		
		this.packetContext = new Context(customObject);
		this.pingTracker = new PingTracker(checkTimeoutMS);
		this.threadsafeState = new Threadsafe();
		
		this.currentState = initialState;
		this.lockCurrentState = new Object();
	}
	
	/**
	 * The {@link PacketContext} that a packet received on this connection will have
	 * @return The connection's context
	 */
	public PacketContext getPacketContext() {
		return packetContext;
	}
	
	/**
	 * If the current state reports that the connection can be opened ({@link NetworkConnectionState#canOpenConnection()}),
	 * the state changes to {@link NetworkConnectionState#OPENING} and the opening procedure is initialized.
	 * <p>
	 * Depending on the connection implementation, the opening process can be synchrounous or asynchrounous.
	 * <ul>
	 * <li>If the connection is internal, it will open immediately and return a completed {@link Task}.</li>
	 * <li>If the connection is made to a remote server over the network, the method returns as soon as
	 * the connection process has been initialized. The returned {@link Task} will complete when the connection has
	 * been confirmed by the remote side.</li>
	 * </ul>
	 * </p><p>
	 * The connection state will change to {@link NetworkConnectionState#OPEN} as soon as the connection is confirmed by
	 * the remote side
	 * </p>
	 * @return A {@link Task} that will complete when the connection is confirmed
	 */
	public Task openConnection() {
		synchronized (lockCurrentState) {
			if(currentState.canOpenConnection()) {
				STATE_LOGGER.debug("Attempting to open connection from %s to %s (At state %s)",
						getLocalID().getDescription(), remoteID.getDescription(), currentState);
				currentState = NetworkConnectionState.OPENING;
				return openConnectionImpl();
			} else {
				STATE_LOGGER.info("Cannot open a connection that is in state %s", currentState);
				return Task.completed();
			}
		}
	}
	
	/**
	 * Will be called when opening. State is already checked and synced.
	 */
	protected abstract Task openConnectionImpl();
	
	/**
	 * Closes this connection. The reason will be {@link ConnectionCloseReason#EXPECTED}.
	 * <p>
	 * When closing a connection, the state changes to {@link NetworkConnectionState#CLOSING}.
	 * In that state, the data receiver thread (if it exists for this implementation) is shut down,
	 * the connection is removed from the server's connection list, and the remote peer is notified 
	 * so that it can also shut down properly. A {@link ConnectionClosedEvent} will be posted to the
	 * relevant network manager.
	 * </p><p>
	 * This method may return before the process of closing the connection is completed.
	 * When that process completes, the state will change to {@link NetworkConnectionState#CLOSED}
	 * and the returned task will complete.
	 * </p>
	 * @return A {@link Task} that will complete when the connection is fully closed. 
	 */
	public Task closeConnection() {
		return closeConnection(ConnectionCloseReason.EXPECTED);
	}
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * Closes the connection for a reason different from {@link ConnectionCloseReason#EXPECTED}.<br>
	 * Use {@link #closeConnection()} to close connection normally.
	 * </p>
	 * @param reason The reason why the connection will be closed
	 * @return A {@link Task} that will complete when the connection is fully closed. 
	 */
	@Internal
	public Task closeConnection(ConnectionCloseReason reason) {
		//This WILL alter the server, so lock on:
		if(networkManager instanceof NetworkManagerServer) {
			NetworkManagerServer serverManager = (NetworkManagerServer) networkManager;
			return serverManager.exclusiveThreadsafe().actionReturn((server) -> {
				return closeConnectionWithServerLock(reason);
			});
		} else {
			return closeConnectionWithServerLock(reason);
		}
		
	}
	
	/**
	 * Called after acquiring the server's exclusive lock (not necessary for clients).
	 * @param reason The reason why the connection will be closed
	 * @return A {@link Task} that will complete when the connection is fully closed. 
	 */
	private Task closeConnectionWithServerLock(ConnectionCloseReason reason) {
		synchronized (lockCurrentState) {
			if(currentState.hasBeenClosed()) {
				STATE_LOGGER.debug("Cannot close a connection that is in state %s", currentState);
				return Task.completed();
			} else {
				STATE_LOGGER.debug("Attempting to close connection from %s to %s (At state %s; Reason %s)",
						getLocalID().getDescription(), remoteID.getDescription(), currentState, reason);
				currentState = NetworkConnectionState.CLOSING;
				return closeConnectionImpl(reason);
			}
		}
	}
	
	/**
	 * Will be called when closing. State is already checked and synced.
	 */
	protected abstract Task closeConnectionImpl(ConnectionCloseReason reason);
	
	/**
	 * Checks whether the connection is still alive by sending a ping signal through the connection.
	 * Can only be called if the current state is {@link NetworkConnectionState#OPEN}.
	 * <p>
	 * Calling this will change the state to {@link NetworkConnectionState#CHECKING}, but data can still be sent.
	 * The state will then change back to {@link NetworkConnectionState#OPEN} if the partner responded, 
	 * or to {@link NetworkConnectionState#CLOSING} if the timeout ({@link #getCheckTimeout()}) expired.
	 * It may also be closed for other reasons during the checking period.
	 * </p><p>
	 * The checking process is not guaranteed to be completed when this method returns: The state after this
	 * method returns can be {@link NetworkConnectionState#CHECKING}, {@link NetworkConnectionState#OPEN}
	 * and {@link NetworkConnectionState#CLOSING}. If the remote peer does not respond, the connection will
	 * begin its closing routine as described in {@link #closeConnection()} as soon as {@link #updateConnectionStatus()}
	 * is called. This can be done automatically by enabling {@link CommonConfig#getGlobalConnectionCheck()}.
	 * </p>
	 * @return A {@link Task} that completes when the remote side has answered the ping
	 * and the state has changed back to {@link NetworkConnectionState#OPEN}
	 */
	public Task checkConnection() {
		synchronized (lockCurrentState) {
			if(currentState == NetworkConnectionState.OPEN) {
				final int pingId = pingTracker.initiatePing();
				final Task waiter = pingTracker.getPingWaiter(pingId);
				STATE_LOGGER.debug("Attempting to check connection from %s to %s (At state %s)",
						getLocalID().getDescription(), remoteID.getDescription(), currentState);
				if(pingId < 0 || !checkConnectionImpl(pingId)) {
					SEND_LOGGER.warning("Connection check failed to send");
					pingTracker.cancelPing(pingId);
				}
				return waiter;
			} else {
				STATE_LOGGER.info("Cannot check connection at state %s", currentState);
				return Task.completed();
			}
		}
	}
	
	/**
	 * Will be called when checking. State is already checked and synced.
	 */
	protected abstract boolean checkConnectionImpl(int uuid);

	/**
	 * The time (in ms) after which no response from the remote side
	 * after calling {@link #checkConnection()} will be considered disconnected.<br>
	 * Any negative value means that no interval was set, and the connection will
	 * wait in the {@link NetworkConnectionState#CHECKING} state indefinitely if
	 * the partner does not respond.
	 * <p>
	 * Can be set in the {@link CommonConfig} of the manager.
	 * @return The timeout in {@code ms} until a connection is considered disconnected
	 */
	public int getCheckTimeout() {
		return pingTracker.timeoutMs;
	}
	
	
	/**
	 * The current {@link NetworkConnectionState} of this connection.<p>
	 * This method is fine for simply viewing the current state, but is not useful
	 * to make decisions based on the current state, because the returned information may be
	 * outdated as soon as the method returns.<br>
	 * Use {@link #threadsafe()} and {@link #getThreadsafeState()} in combination to
	 * avoid concurrent modification of the state.
	 * @return The current connection state
	 */
	public NetworkConnectionState getCurrentState() {
		//Explicitly not synced: Value is always valid, for sync use ThreadsafeAction
		return currentState;
	}
	
	/**
	 * A {@link ThreadsafeAction} object that allows synchronized access to the connection object without
	 * exposing the lock. 
	 * @return A {@link ThreadsafeAction} view for this connection object
	 */
	public ThreadsafeAction<NetworkConnection> threadsafe() {
		return threadsafeState;
	}
	
	/**
	 * The current {@link NetworkConnectionState} of this connection.
	 * <p>
	 * This method can only be used if the caller thread holds the state's monitor.
	 * It is designed to be used inside the {@link #threadsafe()}s methods when
	 * concurrent modification should be prevented.
	 * </p>
	 * @return The current connection state
	 * @throws IllegalStateException If the current thread does not hold the state's monitor
	 */
	public NetworkConnectionState getThreadsafeState() {
		if(Thread.holdsLock(lockCurrentState)) {
			return currentState;
		} else {
			throw new IllegalStateException("Current thread does not hold object monitor");
		}
	}
	
	/**
	 * The {@link NetworkID} of the local side of this connection. Identical to the
	 * local ID of this connection's network manager
	 * @return The local connection ID
	 */
	public NetworkID getLocalID() {
		return networkManager.getLocalID();
	}
	
	/**
	 * The {@link NetworkID} representing the remote side of this connection.
	 * @return The remote connection ID
	 */
	public NetworkID getRemoteID() {
		return remoteID;
	}
	
	/**
	 * The {@link NetworkManagerCommon} that holds or held the connection.<br>
	 * A connection can only ever be held by one manager. After the connection has been
	 * closed, this method will still return the network manager instance that 
	 * once held this connection.
	 * @return The network manager associated with this connection
	 */
	public NetworkManagerCommon getNetworkManager() {
		return networkManager;
	}
	
	/**
	 * Sends a {@link Packet} through this connection. Sending is only possible
	 * in certain connection states ({@link NetworkConnectionState#canSendData()}).
	 * <p>
	 * To increase performance, this method does not acquire the exclusive monitor of the state.
	 * The sending process may fail even if this method returned {@code true} initially.
	 * If sending the packet was not possible, a {@link PacketSendingFailedEvent} will be posted to this
	 * connection's manager
	 * @param packet The packet to send
	 * @return {@code true} if it was attempted to send the packet, {@code false} if it failed
	 * because the connection was in the wrong state. <b>If this method returns {@code false},
	 * no {@link PacketSendingFailedEvent} will be posted</b>
	 */
	public boolean sendPacket(Packet packet) {
		//No sync intentionally for performance, will generate a Sending failed event otherwise
		if(getCurrentState().canSendData()) {
			sendPacketImpl(packet);
			return true;
		} else {
			SEND_LOGGER.info("Packet sending failed for state %s. No event generated", currentState);
			return false;
		}
	}
	
	protected abstract void sendPacketImpl(Packet packet);
	
	/**
	 * Can simulate a received packet on this connection. The packet will go through the
	 * network managers handler chain and can not be distinguished from a packet that was actually received
	 * on the connection.
	 * @param packet The packet that should be handled as received by this connection
	 */
	public void receivePacket(Packet packet) {
		networkManager.receivePacketOnConnectionThread(packet, packetContext);
	}
	
	/**
	 * <p>
	 * The last recorded delay between sending data and receiving the response in milliseconds.
	 * This corresponds to the network ping plus the time to encode and decode the packets.<br>
	 * Will be updated when calling {@link #checkConnection()} and receiving the matching response
	 * </p><p>
	 * This value is the time for request and response, so approximately <b>twice</b> the time
	 * to send a packet to the remote side without awaiting a reply.
	 * </p>
	 * @return The last recorded send/receive delay, or {@code -1} if no delay has been recorded yet
	 */
	public int getLastConnectionDelay() {
		return pingTracker.getLastPingTime();
	}
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * Called when the remote side requested a connection check (ping). The connection implementation will
	 * send the appropriate response.
	 * </p>
	 * @param uuid The id of the received request
	 */
	@Internal
	public abstract void receiveConnectionCheck(int uuid);
	
	/**
	 * Updates this connection's status depending on the last check:
	 * If the ping was returned, the connection stays open, if the
	 * ping timed out, the connection will be closed
	 */
	public void updateConnectionStatus() {
		synchronized (lockCurrentState) {
			if(currentState == NetworkConnectionState.CHECKING && pingTracker.isTimedOut()) {
				closeConnection(ConnectionCloseReason.TIMEOUT);
			}
		}
	}
	
	/**
	 * {@link NetworkConnection}s are always client-to-server connection. This flag indicates which side of
	 * the connection is represented by this object
	 * @return {@code true} if this is the server side of the connection, {@code false} if it is the client side
	 */
	public boolean isServerSide() {
		return isServerSide;
	}
	
	/**
	 * Can be used by implementations of {@link #closeConnectionImpl(ConnectionCloseReason)}
	 * to handle all manager-related close() procedures.
	 * <br>
	 * Call this while the state is CLOSING
	 * @param reason Required
	 * @param exception Optional
	 */
	protected void postEventAndRemoveConnection(ConnectionCloseReason reason, Exception exception) {
		getNetworkManager().getEventDispatcher().post(getNetworkManager().ConnectionClosed,
				new ConnectionClosedEvent(reason, exception, this));
		getNetworkManager().removeConnectionSilently(this);
	}
	
	/**
	 * The {@link PacketContext} for {@link Packet}s received on this connection
	 */
	private class Context implements PacketContext {

		private final Object customData;
		
		private Context(Object customData) {
			this.customData = customData;
		}
		
		@Override
		public NetworkID getLocalID() {
			return NetworkConnection.this.getLocalID();
		}

		@Override
		public NetworkID getRemoteID() {
			return NetworkConnection.this.getRemoteID();
		}

		@Override
		public boolean isServer() {
			return NetworkConnection.this.isServerSide;
		}

		@Override
		public boolean replyPacket(Packet packet) {
			return sendPacket(packet);
		}
		
		@Override
		public Object getCustomData() {
			return customData;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public <T> T getCustomData(Class<T> dataType) {
			return (T) customData;
		}

		@Override
		public NetworkConnection getConnection() {
			return NetworkConnection.this;
		}

		@Override
		public String toString() {
			return "Context [getLocalID()=" + getLocalID() + ", getRemoteID()=" + getRemoteID() + ", isServer()="
					+ isServer() + "]";
		}
	}
	
	protected static final AtomicInteger PING_TRACKER_UUID_GENERATOR = new AtomicInteger(0);
	class PingTracker {
		private final int timeoutMs;
		
		private int cachedPingTime;
		
		private boolean pingActive;
		private long lastPingStartTime;
		private int lastPingUuid;
		private AwaitableTask currentPingWaiter;
		
		private PingTracker(int timeoutMs) {
			this.timeoutMs = timeoutMs;
			this.cachedPingTime = -1; //No time
			reset();
		}
		
		private void reset() {
			if(this.currentPingWaiter != null) this.currentPingWaiter.release();
			this.pingActive = false;
			this.lastPingStartTime = -1;
			this.lastPingUuid = -1;
			this.currentPingWaiter = null;
		}
		
		public int initiatePing() {
			synchronized (lockCurrentState) {
				//Make uuid
				final int uuid = PING_TRACKER_UUID_GENERATOR.getAndIncrement();
				//Set values
				pingActive = true;
				lastPingStartTime = NetworkManager.getClockMillis();
				lastPingUuid = uuid;
				currentPingWaiter = new AwaitableTask();
				//Update state
				NetworkConnection.this.currentState = NetworkConnectionState.CHECKING;
				return uuid;
			}
		}
		
		public Task getPingWaiter(int id) {
			synchronized (lockCurrentState) {
				if(id == lastPingUuid && currentPingWaiter != null) {
					return currentPingWaiter;
				} else {
					STATE_LOGGER.warning("PingTracker: Inconsistent state (attempted to get inactive ping waiter); resetting");
					reset();
					return Task.completed();
				}
			}
		}
		
		public void confirmPing(int id) {
			synchronized (lockCurrentState) {
				if(id == lastPingUuid && currentPingWaiter != null) {
					//Store the time difference
					cachedPingTime = (int) (NetworkManager.getClockMillis() - lastPingStartTime);
					reset();
					NetworkConnection.this.currentState = NetworkConnectionState.OPEN;
				} else {
					STATE_LOGGER.warning("PingTracker: Inconsistent state (attempted to confirm inactive ping); resetting");
					reset();
				}
			}
		}
		
		public void cancelPing(int id) {
			synchronized (lockCurrentState) {
				if(id == lastPingUuid) {
					//Reset
					reset();
					NetworkConnection.this.currentState = NetworkConnectionState.OPEN;
				} else {
					STATE_LOGGER.warning("PingTracker: Inconsistent state (attempted to cancel inactive ping); resetting");
					reset();
				}
			}
		}
		
		public boolean isTimedOut() {	
			return pingActive && (NetworkManager.getClockMillis() - lastPingStartTime > timeoutMs);
		}
		
		public int getLastPingTime() {
			return cachedPingTime;
		}
	}
	
	private class Threadsafe implements ThreadsafeAction<NetworkConnection> {

		@Override
		public void action(Consumer<NetworkConnection> action) {
			synchronized (lockCurrentState) {
				action.accept(NetworkConnection.this);
			}
		}

		@Override
		public <R> R actionReturn(Function<NetworkConnection, R> action) {
			synchronized (lockCurrentState) {
				return action.apply(NetworkConnection.this);
			}
		}
		
	}
}
