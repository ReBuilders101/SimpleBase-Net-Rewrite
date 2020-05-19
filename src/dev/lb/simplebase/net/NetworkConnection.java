package dev.lb.simplebase.net;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.events.ConnectionCheckEvent;
import dev.lb.simplebase.net.events.ConnectionClosedEvent;
import dev.lb.simplebase.net.events.PacketFailedEvent;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;

/**
 * A {@link NetworkConnection} object exists for every client-to-server connection.
 * It can handle opening, closing and checking the status.
 */
@Threadsafe
public abstract class NetworkConnection implements ThreadsafeAction<NetworkConnection> {

	public static final AtomicInteger GLOBAL_CHECK_UUID = new AtomicInteger(0);
	
	protected NetworkConnectionState currentState;
	protected final Object lockCurrentState;
	
	private long pingStartTime;
	private int pingLastValue;
	private int pingCurrentUUID;
	private final Object lockPing;
	
	private final Context packetContext;
	private final NetworkManagerCommon networkManager;
	private final PacketConverter converter;
	private final NetworkID localID;
	private final NetworkID remoteID;
	private final int checkTimeoutMS;
	private final boolean isServerSide;
	
	protected NetworkConnection(NetworkID localID, NetworkID remoteID, NetworkManagerCommon networkManager,
			NetworkConnectionState initialState, int checkTimeoutMS, boolean serverSide, Object customObject) {
		Objects.requireNonNull(localID, "'localID' parameter must not be null");
		Objects.requireNonNull(remoteID, "'remoteID' parameter must not be null");
		Objects.requireNonNull(networkManager, "'networkManager' parameter must not be null");
		Objects.requireNonNull(initialState, "'initialState' parameter must not be null");
		
		this.localID = localID;
		this.remoteID = remoteID;
		this.networkManager = networkManager;
		this.packetContext = new Context(customObject);
		this.checkTimeoutMS = checkTimeoutMS;
		this.isServerSide = serverSide;
		this.currentState = initialState;
		this.lockCurrentState = new Object();
		this.converter = new PacketConverter(networkManager, this);
		
		this.pingLastValue = -1;
		this.pingStartTime = -1;
		this.pingCurrentUUID = -1;
		this.lockPing = new Object();
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
				NetworkManager.NET_LOG.info("Attempting to open connection from %s to %s (At state %s)", localID, remoteID, currentState);
				openConnectionImpl();
				return true;
			} else {
				NetworkManager.NET_LOG.warning("Cannot open a connection that is in state " + currentState);
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
		synchronized (lockCurrentState) {
			if(currentState.hasBeenClosed()) {
				return false;
			} else {
				NetworkManager.NET_LOG.info("Attempting to close connection from %s to %s (At state %s)", localID, remoteID, currentState);
				closeConnectionImpl();
				return true;
			}
		}
	}
	
	/**
	 * Will be called when closing. State is already checked and synced.
	 */
	protected abstract void closeConnectionImpl();
	
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
	 * begin its closing routine as described in {@link #closeConnection()}. If the check succeeds, a {@link ConnectionCheckEvent}
	 * will be posted to this connections manager.
	 * @return  {@code true} if checking the connection was <b>attempted</b>, {@code false} if it was not attempted
	 * because the connection was in a state where this is not possible.
	 * The returned value does not contain any information about the success of the connection check.
	 */
	public boolean checkConnection() {
		synchronized (lockCurrentState) {
			if(currentState == NetworkConnectionState.OPEN) {
				synchronized (lockPing) { //We edit the ping here
					currentState = NetworkConnectionState.CHECKING;
					int uuid = GLOBAL_CHECK_UUID.getAndIncrement();
					if(pingCurrentUUID != -1 || pingStartTime != -1) NetworkManager.NET_LOG.info("Initialized check request while previous was unsanswered");
					pingCurrentUUID = uuid;
					pingStartTime = System.currentTimeMillis();
					NetworkManager.NET_LOG.info("Attempting to check connection from %s to %s (At state %s)", localID, remoteID, currentState);
					checkConnectionImpl(uuid);
				}
				return true;
			} else {
				return false;
			}
 		}
	}
	
	/**
	 * Will be called when checking. State is already checked and synced.
	 */
	protected abstract void checkConnectionImpl(int uuid);

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
		return checkTimeoutMS;
	}
	
	/**
	 * Ensures that the state of the connection is not altered while the {@link Consumer}
	 * is running.<p>
	 * In some cases, the actual state is changed by external effects, such as a remote partner
	 * closing the connection. In those cases, The value of the state variable will only be
	 * updated <b>after</b> this method completes. Because of this, the state variable is
	 * never guaranteed to exactly correspond to the undelying network object's state.<br>
	 * These cases are relatively rare, and it is safe to assume that the current state is the actual one.
	 * <p>
	 * <i>Original method documentation:</i><br>
	 * {@inheritDoc}
	 */
	@Override
	public void action(Consumer<NetworkConnection> action) {
		synchronized (lockCurrentState) {
			action.accept(this);
		}
	}

	/**
	 * Ensures that the state of the connection is not altered while the {@link Function}
	 * is running, returning the function's result.<p>
	 * In some cases, the actual state is changed by external effects, such as a remote partner
	 * closing the connection. In those cases, The value of the state variable will only be
	 * updated <b>after</b> this method completes. Because of this, the state variable is
	 * never guaranteed to exactly correspond to the undelying network object's state.<br>
	 * These cases are relatively rare, and it is safe to assume that the current state is the actual one.
	 * <p>
	 * <i>Original method documentation:</i><br>
	 * {@inheritDoc}
	 */
	@Override
	public <R> R actionReturn(Function<NetworkConnection, R> action) {
		synchronized (lockCurrentState) {
			return action.apply(this);
		}
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
		return currentState;
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
		return localID;
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
	 * If sending the packet was not possible, a {@link PacketFailedEvent} will be posted to this
	 * connection's manager
	 * @param packet The packet to send
	 * @return {@code true} if it was attempted to send the packet, {@code false} if it failed
	 * because the connection was in the wrong state. <b>If this method returns {@code false},
	 * no {@link PacketFailedEvent} will be posted</b>
	 */
	public boolean sendPacket(Packet packet) {
		//No sync intentionally
		if(getCurrentState().canSendData()) {
			sendPacketImpl(packet);
			return true;
		} else {
			return false;
		}
	}
	
	protected abstract void sendPacketImpl(Packet packet);
	
	@Internal
	protected PacketContext getContext() {
		return packetContext;
	}
	
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
		synchronized (lockPing) {
			return pingLastValue;
		}
	}
	
	protected abstract void receiveConnectionCheck(int uuid);
	
	protected void receiveConnectionCheckReply(int uuid) {
		synchronized (lockCurrentState) {
			synchronized (lockPing) { //Dealing with the ping
				if(pingCurrentUUID != uuid) {
					NetworkManager.NET_LOG.info("Ping uuid mismatch. Ping response ignored, UUID reset");
				} else if(pingLastValue == -1) {
					NetworkManager.NET_LOG.info("No recorded ping start time. Ping response ignored");
				} else {
					final long time = System.currentTimeMillis();
					pingLastValue = (int) (time - pingStartTime); //calc the total time
				}
				pingCurrentUUID = -1;
				pingStartTime = -1;
				currentState = NetworkConnectionState.OPEN;
			}
		}
	}
	
	protected PacketConverter getConverter() {
		return converter;
	}
	
	protected void updateCheckTimeout() {
		synchronized (lockCurrentState) {
			if(currentState == NetworkConnectionState.CHECKING) {
				synchronized (lockPing) {
					if(pingStartTime == -1) return;
					final long currentTimeout = System.currentTimeMillis() - pingStartTime;
					if(currentTimeout > checkTimeoutMS) {
						closeTimeoutImpl();
					}
				}
			}
		}
	}
	
	/**
	 * Handles a connection closing because the check timeout expired
	 */
	protected abstract void closeTimeoutImpl();
	
	protected abstract void receiveUDPLogout();
	
	/**
	 * {@link NetworkConnection}s are always client-to-server connection. This flag indicates which side of
	 * the connection is represented by this object
	 * @return {@code true} if this is the server side of the connection, {@code false} if it is the client side
	 */
	public boolean isServerSide() {
		return isServerSide;
	}
	
	//The context
	private class Context implements PacketContext {

		private final Object customData;
		
		private Context(Object customData) {
			this.customData = customData;
		}
		
		@Override
		public NetworkID getLocalID() {
			return localID;
		}

		@Override
		public NetworkID getRemoteID() {
			return remoteID;
		}

		@Override
		public boolean isServer() {
			return isServerSide;
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
}
