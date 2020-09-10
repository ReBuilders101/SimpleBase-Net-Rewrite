package dev.lb.simplebase.net.events;

import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.event.Event;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;

public class ConnectionClosedEvent extends Event {

	private final ConnectionCloseReason reason;
	private final Exception exception;
	private final NetworkID remoteId;
	private final NetworkManagerCommon manager;
	private final boolean serverSide;
	
	public ConnectionClosedEvent(ConnectionCloseReason reason, Exception exception, NetworkConnection connection) {
		super(false, false);
		this.reason = reason;
		this.exception = exception;
		this.remoteId = connection.getRemoteID();
		this.manager = connection.getNetworkManager();
		this.serverSide = connection.isServerSide();
	}
	
	public ConnectionCloseReason getReason() {
		return reason;
	}
	
	public Exception getException() {
		return exception;
	}
	
	public void rethrowException() throws Exception {
		if(exception != null) throw exception;
	}

	public NetworkID getDisconnectedId() {
		return remoteId;
	}
	
	public NetworkManagerCommon getNetworkManager() {
		return manager;
	}
	
	public boolean isServerSide() {
		return serverSide;
	}
	
}
