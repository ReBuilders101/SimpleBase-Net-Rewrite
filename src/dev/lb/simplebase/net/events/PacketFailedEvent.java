package dev.lb.simplebase.net.events;

import dev.lb.simplebase.net.event.Event;

public class PacketFailedEvent extends Event {

	//Cancel to prevent warning
	protected PacketFailedEvent() {
		super(true, false);
	}
}
