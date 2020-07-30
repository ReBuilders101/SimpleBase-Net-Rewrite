package dev.lb.simplebase.net.events;

import dev.lb.simplebase.net.event.Event;

public class PacketSendingFailedEvent extends Event {

	//Cancel to prevent warning
	protected PacketSendingFailedEvent() {
		super(true, false);
	}
}
