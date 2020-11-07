package dev.lb.simplebase.net.manager;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import dev.lb.simplebase.net.connection.ChannelConnection;

/**
 * An interface implemented by {@link NetworkManagerServer}s that provide a NIO {@link Selector} that
 * connection channels can be registered for
 */
public interface SelectorManager {

	/**
	 * Registers a connection's channel with the server' selector
	 * @param channel The {@link SelectableChannel} to register
	 * @param ops The operation flags to register for
	 * @param connection The {@link ChannelConnection} that should be attched to the selection key
	 * @return The {@link SelectionKey} for the registration
	 */
	public SelectionKey registerConnection(SelectableChannel channel, int ops, ChannelConnection connection);
	
}
