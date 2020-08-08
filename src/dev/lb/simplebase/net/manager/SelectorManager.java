package dev.lb.simplebase.net.manager;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public interface SelectorManager {

	public SelectionKey registerConnection(SocketChannel channel, int ops);
	
}
