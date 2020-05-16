package dev.lb.simplebase.net;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.id.NetworkID;

public abstract class NetworkManagerServer extends NetworkManagerCommon {

	private final Map<NetworkID, NetworkConnection> connections;
	private final ReadWriteLock lockConnections;
	
	protected NetworkManagerServer(NetworkID local, ServerConfig config) {
		super(local, config);
		this.connections = new HashMap<>();
		this.lockConnections = new ReentrantReadWriteLock();
		
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServerConfig getConfig() {
		return (ServerConfig) super.getConfig();
	}

}
