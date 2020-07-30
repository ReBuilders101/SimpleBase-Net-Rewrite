package dev.lb.simplebase.net;

import java.net.Socket;
import java.util.Objects;

/**
 * Stores the current state of a {@link NetworkConnection} in its lifecycle.
 * The lifecycle of a connection has this basic structure:
 * <ol>
 * <li>After creation, the connection will have the {@link #INITIALIZED} state. No data receiver thread will be active with this state</li>
 * <li>By calling {@link NetworkConnection#openConnection()}, the connection will switch into the {@link #OPENING} state.
 * 	   In this phase, the data receiver thread will be activated and will run during the following phases. If completing the
 *     connection fails, the state will change to {@link #CLOSING} or {@link #CLOSED} instead.<br>
 *     A connection can also start in the {@link #OPENING} state if it was created by a server socket accepting a connection</li>
 * <li>After the connection is established, the state will be set to {@link #OPEN}. Some connection implementations can
 * 	   skip step 2 and transition directly from {@link #INITIALIZED} to {@link #OPEN}</li>
 * <li>The connection state can change from {@link #OPEN} to {@link #CHECKING} by calling {@link NetworkConnection#checkConnection()}.
 * 	   This sends a ping signal to test whether the remote partner is still active. If the remote partner responds, the state
 *     will change back to the {@link #OPEN} state</li>
 * <li>The connection changes to {@link #CLOSING} when
 * 		<ul><li>The network connection is closed by calling {@link NetworkConnection#closeConnection()}</li>
 * 		<li>The network connection is closed from the remote side</li>
 * 		<li>The data receiver thread is interrupted</li>
 * 		<li>An unrecoverable exception is encountered while receiving data from the connection</li>
 * 		<li>The remote partner does not respond during the {@link #CHECKING} state and the
 * 		connection is considered to be disconnected</li>
 * 		</ul>
 * 	   During this phase, the data receiver thread will stop running and the connection will
 * 	   be removed from the manager</li>
 * <li>The {@link #CLOSED} state is the last state for any connection. It cannot change to any other state from here.
 * 	   A connection in this state can only be encountered when a reference is maintained outside of the network manager,
 * 	   as the connection instance is removed from the manager's connection pool during the {@link #CLOSING} phase
 * </li></ol> 
 */
public enum NetworkConnectionState {

	INITIALIZED(false, true, false, false),
	OPENING(false, false, true, false),
	OPEN(true, false, true, false),
	CHECKING(true, false, true, false),
	CLOSING(false, false, true, true),
	CLOSED(false, false, true, true);
	
	private final boolean send, can, open, closed;
	
	private NetworkConnectionState(boolean send, boolean can, boolean open, boolean closed) {
		this.send = send;
		this.can = can;
		this.open = open;
		this.closed = closed;
	}

	/**
	 * Whether it is possible to send data over a connection with this state.<br>
	 * Will be {@code true} for these states: {@link #OPEN}, {@link #CHECKING}.
	 * @return {@code true} if data can be sent for this state
	 */
	public boolean canSendData() {
		return send;
	}
	
	/**
	 * Whether it is possible to switch this connection from the current state into a state
	 * where data can be sent ({@link #canSendData()}).<br>
	 * Will be {@code true} for the state {@link #INITIALIZED} only.
	 * @return {@code true} if it is possible to open the connection
	 */
	public boolean canOpenConnection() {
		return can;
	}
	
	/**
	 * Whether the connection has been opened before (or is opening). It will continue to return
	 * {@code true} after the connection has been closed.<br>
	 * Will be {@code true} for these states: {@link #OPENING}, {@link #OPEN}, {@link #CHECKING}, {@link #CLOSING}, {@link #CLOSED}.
	 * @return {@code true} if the connection has been opened
	 */
	public boolean hasBeenOpened() {
		return open;
	}
	
	/**
	 * Whether the connection has been closed or is closing.<br>
	 * Will be {@code true} for these states: {@link #CLOSING}, {@link #CLOSED}.
	 * @return {@code true} if the connection has been closed
	 */
	public boolean hasBeenClosed() {
		return closed;
	}
	
	/**
	 * Finds the {@link NetworkConnectionState} that best represents the
	 * state of a {@link Socket}.
	 * @param socket The socket. Must not be {@code null}.
	 * @return The matching network connection state
	 * @throws NullPointerException If socket is {@code null}
	 */
	public static NetworkConnectionState fromSocket(Socket socket) {
		Objects.requireNonNull(socket, "'socket' parameter must not be null");
		if(!socket.isConnected()) return INITIALIZED;
		if(!socket.isClosed()) return OPEN;
		return CLOSED;
	}
	
	/**
	 * Checks that the socket is open and returns the {@link #OPEN} state.
	 * @param socket The socket. Must not be {@code null}.
	 * @return The matching network connection state
	 * @throws IllegalStateException If the socket is closed or not connected
	 * @throws NullPointerException If socket is {@code null}
	 */
	public static NetworkConnectionState assertOpen(Socket socket) {
		Objects.requireNonNull(socket, "'socket' parameter must not be null");
		if(socket.isConnected() && !socket.isClosed()) {
			return OPEN;
		} else {
			throw new IllegalStateException("Socket must be connected and not closed");
		}
	}
	
}
