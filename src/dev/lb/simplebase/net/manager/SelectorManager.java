package dev.lb.simplebase.net.manager;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import dev.lb.simplebase.net.connection.ChannelConnection;

public interface SelectorManager {

	public SelectionKey registerConnection(SelectableChannel channel, int ops, ChannelConnection connection);
	
}
