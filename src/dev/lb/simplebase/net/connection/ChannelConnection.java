package dev.lb.simplebase.net.connection;

import dev.lb.simplebase.net.annotation.Internal;

/**
 * This interface is implemented by {@link NetworkConnection} that are channel-based.<br>
 * They declare a single method that signals to the connection that the channel is ready to read data.
 */
@Internal
public interface ChannelConnection {

	/**
	 * Tells the connection object that data can be read from the associated channel.<br>
	 * Usually called from the manager's selector thread.
	 */
	public void readNow();
	
}
