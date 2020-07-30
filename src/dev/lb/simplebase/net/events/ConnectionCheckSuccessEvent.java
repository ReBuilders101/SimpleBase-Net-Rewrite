package dev.lb.simplebase.net.events;

import dev.lb.simplebase.net.event.Event;

public class ConnectionCheckSuccessEvent extends Event {

	protected ConnectionCheckSuccessEvent(boolean cancelledInitially) {
		super(false, cancelledInitially);
	}
	
}
