package dev.lb.simplebase.net.events;

import dev.lb.simplebase.net.event.Event;

public class ConnectionClosedEvent extends Event {

	public ConnectionClosedEvent(ConnectionCloseReason reason, Exception exception) {
		super(false, false);
	}
	
	

}
