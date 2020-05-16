package dev.lb.simplebase.net;

import dev.lb.simplebase.net.event.Event;

public class ConnectionCheckEvent extends Event {

	protected ConnectionCheckEvent(boolean cancelledInitially) {
		super(false, cancelledInitially);
	}
	
}
