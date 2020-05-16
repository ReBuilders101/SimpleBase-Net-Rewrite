package dev.lb.simplebase.net.events;

import dev.lb.simplebase.net.event.Event;

public class UnknownConnectionlessPacketEvent extends Event {

	//Cancel to prevent warning
	protected UnknownConnectionlessPacketEvent() {
		super(true, false);
	}

}
