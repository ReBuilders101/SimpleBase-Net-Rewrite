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
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.util.ThreadsafeAction;

/**
 * A {@link NetworkConnection} object exists for every client-to-server connection.
 * It can handle opening, closing and checking the status.
 */
@Threadsafe
public abstract class NetworkConnection {
	protected static final AbstractLogger RECEIVE_LOGGER = NetworkManager.getModuleLogger("connection-receive");
	protected static final AbstractLogger SEND_LOGGER = NetworkManager.getModuleLogger("connection-send");
	protected static final AbstractLogger STATE_LOGGER = NetworkManager.getModuleLogger("connection-state");
	
	//Sync/state
	protected NetworkConnectionState currentState;
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
	 * Opens the connection to the remote partner. Opening can only happen from {@link NetworkConnectionState#INITIALIZED},
	 * for all other states if will fail. If opening is attempted, the connection will move to the {@link NetworkConnectionState#OPENING} state.
	 * <p>
	 * The opening process is not guaranteed to be completed when this method returns: The state after this method returns
	 * can be {@link NetworkConnectionState#OPENING} if opening was not completed, but is still ongoing, {@link NetworkConnectionState#OPEN}
	 * if opening was completed, and {@link NetworkConnectionState#CLOSING} or 
	 * {@link NetworkConnectionState#CLOSED} when opening the connection failed.
	 * <p>
	 * When checking state before/after calling this method, make sure to do this in an {@link #action(Consumer)} block to
	 * ensure thread safety.
	 * @return {@code true} if opening the connection was <b>attempted</b>, {@code false} if it was not attempted
	 * because the connection was in a state where this is not possible (See {@link NetworkConnectionState#canOpenConnection()}).
	 * The returned value does not contain any information about the success of an attempt to establish the connection.
	 */
	public boolean openConnection() {
		synchronized (lockCurrentState) {
			if(currentState.canOpenConnection()) {
				STATE_LOGGER.debug("Attempting to open connection from %s to %s (At state %s)", getLocalID(), remoteID, currentState);
				openConnectionImpl();
				return true;
			} else {
				STATE_LOGGER.info("Cannot open a connection that is in state %s", currentState);
				return false;
			}
		}
	}
	
	/**
	 * Will be called when opening. State is already checked and synced.
	 */
	protected abstract void openConnectionImpl();
	
	/**
	 * Closes the connection to the remote partner, or marks this connection as closed if the
	 * connection was never opened. A connection can be closed from any state except {@link NetworkConnectionState#CLOSED}
	 * or {@link NetworkConnectionState#CLOSING} (because that means the connection is currently closing / has already been closed.
	 * <p>
	 * The closing process is not guaranteed to be completed when this method returns: The state after this method returns
	 * can be {@link NetworkConnectionState#CLOSING} of the closing process is not done, or {@link NetworkConnectionState#CLOSED}
	 * if the process was completed.
	 * <p>
	 * Closing the connection will automatically remove it from the {@link NetworkManager}'s connection list and post a
	 * {@link ConnectionClosedEvent} to that manager when the closing process is completed.
	 * @return {@code true} if closing the connection was <b>attempted</b>, {@code false} if it was not attempted
	 * because the connection was in a state where this is not possible because it already has been
	 * closed (See {@link NetworkConnectionState#hasBeenClosed()}).
	 * The returned value does not contain any information about the success of an attempt to close the connection.
	 */
	public boolean closeConnection() {
		return closeConnection(ConnectionCloseReason.EXPECTED);
	}
	
	/**
	 * Used during server stopping to set a different state than EXPECTED
	 */
	@Internal
	protected boolean closeConnection(ConnectionCloseReason reason) {
		synchronized (lockCurrentState) {
			if(currentState.hasBeenClosed()) {
				STATE_LOGGER.debug("Cannot close a connection that is in state %s", currentState);
				return false;
			} else {
				STATE_LOGGER.debug("Attempting to close connection from %s to %s (At state %s)", getLocalID(), remoteID, currentState);
				closeConnectionImpl(reason);
				return true;
			}
		}
	}
	
	/**
	 * Will be called when closing. State is already checked and synced.
	 */
	protected abstract void closeConnectionImpl(ConnectionCloseReason reason);
	
	/**
	 * Checks whether the connection is still alive by sending a ping signal through the connection.
	 * Can only be called if the current state is {@link NetworkConnectionState#OPEN}.
	 * <p>
	 * Calling this will change the state to {@link NetworkConnectionState#CHECKING}, but data can still be sent.
	 * The state will then change back to {@link NetworkConnectionState#OPEN} if the partner responded, 
	 * or to {@link NetworkConnectionState#CLOSING} if the timeout ({@link #getCheckTimeout()}) expired.
	 * It may also be closed for other reasons during the checking period.
	 * <p>
	 * The checking process is not guaranteed to be completed when this method returns: The state after this
	 * method returns can be {@link NetworkConnectionState#CHECKING}, {@link NetworkConnectionState#OPEN}
	 * and {@link NetworkConnectionState#CLOSING}. If the remote peer does not respond, the connection will
	 * begin its closing routine as described in {@link #closeConnection()}. If the check succeeds, a {@link ConnectionCheckSuccessEvent}
	 * will be posted to this connections manager.
	 * @return  {@code true} if checking the connection was <b>attempted</b>, {@code false} if it was not attempted
	 * because the connection was in a state where this is not possible.
	 * The returned value does not contain any information about the success of the connection check.
	 */
	public boolean checkConnection() {
		synchronized (lockCurrentState) {
			if(currentState == NetworkConnectionState.OPEN) {
					final int pingId = pingTracker.initiatePing();
					STATE_LOGGER.debug("Attempting to check connection from %s to %s (At state %s)", getLocalID(), remoteID, currentState);
					if(pingId < 0 || !checkConnectionImpl(pingId)) {
						SEND_LOGGER.warning("Connection check failed to send");
						pingTracker.cancelPing(pingId);
					}
				return true;
			} else {
				STATE_LOGGER.info("Cannot check connection at state %s", currentState);
				return false;
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
	 * Use {@link #action(Consumer)} and {@link #getThreadsafeState()} in combination to
	 * avoid concurrent modification of the state.
	 * @return The current connection state
	 */
	public NetworkConnectionState getCurrentState() {
		synchronized (lockCurrentState) {
			return currentState;
		}
	}
	
	public ThreadsafeAction<NetworkConnection> threadsafe() {
		return threadsafeState;
	}
	
	/**
	 * The current {@link NetworkConnectionState} of this connection.<p>
	 * This method can only be used if the caller thread holds the state's monitor.
	 * It is designed to be used inside a {@link #action(Consumer)} block when
	 * concurrent modification should be prevented.
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
	 * The {@link NetworkID} of the local side of this connection. Identical to this
	 * network managers local ID.
	 * @return The local connection ID
	 */
	public NetworkID getLocalID() {
		return networkManager.getLocalID();
	}
	
	/**
	 * The {@link NetworkID} of the remote side of this connection.
	 * @return The remote sides connection ID
	 */
	public NetworkID getRemoteID() {
		return remoteID;
	}
	
	/**
	 * The {@link NetworkManagerCommon} that holds or held the connection.<br>
	 * A connection can only ever be held by one manager. After the connection has been
	 * closed, this method will still return the Network Manager instance that 
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
		//No sync intentionally
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
	 * The last recorded delay between sending data and receiving the response in milliseconds.
	 * This corresponds to the network ping plus the time to encode and decode the packets.<br>
	 * Will be updated when calling {@link #checkConnection()} and receiving the matching response
	 * <p>
	 * This value is the time for request and response, so approximately <b>twice</b> the time
	 * to send a packet to the remote side without awaiting a reply.
	 * @return The last recorded send/receive delay, or {@code -1} if no delay has been recorded yet
	 */
	public int getLastConnectionDelay() {
		return pingTracker.getLastPingTime();
	}
	
	/**
	 * The remote partner has sent a check message.
	 * Reply with a check reply message
	 * @param uuid
	 */
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
				new ConnectionClosedEvent(reason, exception));
		getNetworkManager().removeConnectionSilently(this);
	}
	
	//The context
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
	}
	
	protected static final AtomicInteger PING_TRACKER_UUID_GENERATOR = new AtomicInteger(0);
	class PingTracker {
		private final int timeoutMs;
		
		private int cachedPingTime;
		
		private boolean pingActive;
		private long lastPingStartTime;
		private int lastPingUuid;
		
		private PingTracker(int timeoutMs) {
			this.timeoutMs = timeoutMs;
			this.cachedPingTime = -1; //No time
			reset();
		}
		
		private void reset() {
			this.pingActive = false;
			this.lastPingStartTime = -1;
			this.lastPingUuid = -1;
		}
		
		public int initiatePing() {
			synchronized (lockCurrentState) {
				//Make uuid
				final int uuid = PING_TRACKER_UUID_GENERATOR.getAndIncrement();
				//Set values
				pingActive = true;
				lastPingStartTime = NetworkManager.getClockMillis();
				lastPingUuid = uuid;
				//Update state
				NetworkConnection.this.currentState = NetworkConnectionState.CHECKING;
				return uuid;
			}
		}
		
		public void confirmPing(int id) {
			synchronized (lockCurrentState) {
				if(id == lastPingUuid) {
					//Store the time difference
					cachedPingTime = (int) (NetworkManager.getClockMillis() - lastPingStartTime);
					reset();
					NetworkConnection.this.currentState = NetworkConnectionState.OPEN;
				} else {
					STATE_LOGGER.warning("PingTracker: Inconsistent state (attempted to confirm inactive ping); resetting");
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
