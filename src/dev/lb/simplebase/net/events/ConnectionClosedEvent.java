package dev.lb.simplebase.net.events;

import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.event.Event;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.manager.NetworkManagerClient;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.manager.NetworkManagerServer;

/**
 * This event will be fired for the {@link NetworkManagerCommon#ConnectionClosed} accessor after a connection
 * has been closed.
 * <p>
 * Both closing the conncetion manually and receiving a close signal from the remote side will trigger this event. 
 * </p>
 */
public class ConnectionClosedEvent extends Event {

	private final ConnectionCloseReason reason;
	private final Exception exception;
	private final NetworkID remoteId;
	private final NetworkManagerCommon manager;
	private final boolean serverSide;
	
	/**
	 * Constructs a new {@link ConnectionClosedEvent} with a reson, an optional exception and the connection that was closed
	 * @param reason The {@link ConnectionCloseReason} why the event was fired
	 * @param exception An {@link Exception} that is the cause of the connection closing. May be {@code null} if no exception occurred 
	 * @param connection The {@link NetworkConnection} that was closed
	 */
	public ConnectionClosedEvent(ConnectionCloseReason reason, Exception exception, NetworkConnection connection) {
		super(false, false);
		this.reason = reason;
		this.exception = exception;
		this.remoteId = connection.getRemoteID();
		this.manager = connection.getNetworkManager();
		this.serverSide = connection.isServerSide();
	}
	
	/**
	 * The reason why the connection was closed and the event was fired.
	 * @return The reason why the connection was closed
	 */
	public ConnectionCloseReason getReason() {
		return reason;	
	}
	
	/**
	 * If the connection was closed because of an exception, the cause will be returned.
	 * Otherwise, this method returns {@code null}
	 * @return The {@link Exception} that caused the connection to be closed
	 */
	public Exception getException() {
		return exception;
	}
	
	/**
	 * If {@link #getException()} is not {@code null}, then this exception is thrown by this method
	 * @throws Exception The exception returned by {@link #getException()}
	 */
	public void rethrowException() throws Exception {
		if(exception != null) throw exception;
	}

	/**
	 * The remote {@link NetworkID} of the connection that was closed
	 * @return The remote {@link NetworkID} of the connection that was closed
	 */
	public NetworkID getDisconnectedId() {
		return remoteId;
	}
	
	/**
	 * The {@link NetworkManagerCommon} for which the event was fired.
	 * <p>
	 * Can be cast to {@link NetworkManagerClient} when {@link #isServerSide()} is {@code false}
	 * and to {@link NetworkManagerServer} when {@link #isServerSide()} is {@code true}.
	 * </p>
	 * @return The {@link NetworkManagerCommon} for which the event was fired
	 */
	public NetworkManagerCommon getNetworkManager() {
		return manager;
	}
	
	/**
	 * Whether the event was fired from a client manager ({@link NetworkManagerClient}) or
	 * a server manager ({@link NetworkManagerServer}).
	 * @return {@code true} if the event was fired on a server, {@code false} if the event was fired on a client
	 */
	public boolean isServerSide() {
		return serverSide;
	}
	
}
