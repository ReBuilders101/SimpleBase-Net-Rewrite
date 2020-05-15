package dev.lb.simplebase.net.event;

import dev.lb.simplebase.net.annotation.Threadsafe;

/**
 * The base class for any event implementation.
 */
@Threadsafe
public abstract class Event {

	//make this threadsafe, hopefully. Should be good since everything is a single access, no check and write that would need sync
	private volatile boolean cancelled;
	private final boolean canCancel;
	
	/**
	 * Creates a new event base instance
	 * @param canCancel Whether this event can be cancelled
	 * @param The flag of the cancelled status when the event is posted
	 */
	protected Event(boolean canCancel, boolean cancelledInitially) {
		this.canCancel = canCancel;
		this.cancelled = cancelledInitially;
	}
	
	/**
	 * Tries to set the cancelled status of the event if possible.<p>
	 * This method is not supported for all types of event. Use {@link #canCancel()} to check whether this event can be cancelled.
	 * @param value The new value for the cancellled status
	 * @throws UnsupportedOperationException If this event type does not support cancellation
	 */
	public void setCancelled(boolean value) {
		if(canCancel) {
			cancelled = value;
		} else {
			throw new UnsupportedOperationException("Cannot cancel this type of event: " + getClass().getCanonicalName());
		}
	}
	
	/**
	 * The state of the cancelled flag of this event
	 * @return {@code true} if the event has been cancelled, {@code false} otherwise
	 */
	public boolean isCancelled() {
		return cancelled;
	}
	
	/**
	 * Some events (usually those posted before the action associated with it is executed) can be
	 * cancelled to tell the poster that the action should not be executed. Supporting event cancellation is optional
	 * @return {@code true} if this event can be cancelled, {@code false} otherwise
	 */
	public boolean canCancel() {
		return canCancel;
	}
	
}
