package dev.lb.simplebase.net;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.id.NetworkID;

/**
 * A {@link NetworkConnection} object exists for every client-to-server connection.
 * It can handle opening, closing and checking the status.
 * <p>
 * The class is one of the connection points between public and internal API:<br>
 * Most functions provided can be done by calling the corresponding functions on the
 * network manager with the matching {@link NetworkID}, but all public methods
 * of this class are also safe to use.
 */
@Threadsafe
public abstract class NetworkConnection implements ThreadsafeAction<NetworkConnection> {

	protected NetworkConnectionState currentState;
	protected final Object lockCurrentState;
	
	private final NetworkManagerCommon networkManager;
	private final NetworkID localID;
	private final NetworkID remoteID;
	
	protected NetworkConnection(NetworkID localID, NetworkID remoteID, NetworkManagerCommon networkManager, NetworkConnectionState initialState) {
		Objects.requireNonNull(localID, "'localID' parameter must not be null");
		Objects.requireNonNull(remoteID, "'remoteID' parameter must not be null");
		Objects.requireNonNull(networkManager, "'networkManager' parameter must not be null");
		Objects.requireNonNull(initialState, "'initialState' parameter must not be null");
		
		this.localID = localID;
		this.remoteID = remoteID;
		this.networkManager = networkManager;
		this.currentState = initialState;
		this.lockCurrentState = new Object();
	}
	
	/**
	 * Opens the connection to the remote partner. Opening can only happen from {@link NetworkConnectionState#INITIALIZED},
	 * for all other states if will fail. If opening is attempted, the connection will move to the {@link NetworkConnectionState#OPENING} state.
	 * <p>
	 * The opening process is not guaranteed to be completed when this method returns: The state after this method returns
	 * can be {@link NetworkConnectionState#OPENING} if opening was not completed, but is still ongoing, {@link NetworkConnectionState#OPEN}
	 * if opening was completed, and {@link NetworkConnectionState#CLOSING} when opening the connection failed.
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
				closeConnectionImpl();
				return true;
			}
		}
	}
	
	/**
	 * Will be called when closing. State is already checked and synced.
	 */
	protected abstract void closeConnectionImpl();
	
	
	public void checkConnection() {
		
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
	 * The {@link NetworkID} of the remote side of this connection. Contains the 
	 * remote 
	 * @return
	 */
	public NetworkID getRemoteID() {
		return remoteID;
	}
	
	public NetworkManagerCommon getNetworkManager() {
		return networkManager;
	}
}
