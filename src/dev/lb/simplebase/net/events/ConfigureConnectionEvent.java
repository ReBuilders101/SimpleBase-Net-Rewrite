package dev.lb.simplebase.net.events;

import dev.lb.simplebase.net.NetworkManagerServer;
import dev.lb.simplebase.net.event.Event;
import dev.lb.simplebase.net.id.NetworkID;


public class ConfigureConnectionEvent extends Event {

	private final NetworkID remoteId;
	private final NetworkManagerServer server;
	
	private Object customObject;
	
	public ConfigureConnectionEvent(NetworkManagerServer server, NetworkID remoteId) {
		super(true, false);
		this.server = server;
		this.remoteId = remoteId;
		this.customObject = null;
	}

	public NetworkManagerServer getServer() {
		return server;
	}
	
	public NetworkID getLocalId() {
		return server.getLocalID();
	}
	
	public NetworkID getRemoteId() {
		return remoteId;
	}
	
	public Object getCustomObject() {
		return customObject;
	}
	
	public void setCustomObject(Object data) {
		this.customObject = data;
	}
	
}
