package dev.lb.simplebase.net.events;

import java.util.concurrent.RejectedExecutionException;

import dev.lb.simplebase.net.event.Event;

public class PacketSendingFailedEvent extends Event {

	//Cancel to prevent warning
	public PacketSendingFailedEvent(RejectedExecutionException e) {
		super(true, false);
	}
}
