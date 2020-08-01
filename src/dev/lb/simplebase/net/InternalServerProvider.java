package dev.lb.simplebase.net;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.connection.InternalNetworkConnection;
import dev.lb.simplebase.net.events.ConfigureConnectionEvent;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.manager.NetworkManagerServer;

/**
 * Lists all available internal servers
 */
@Internal
class InternalServerProvider {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("internal-servers");
	
	private static final Map<NetworkID, NetworkManagerServer> serverList = new HashMap<>();
	
	protected static synchronized InternalNetworkConnection createServerPeer(InternalNetworkConnection source) {
		final NetworkManagerServer server = serverList.get(source.getRemoteID());
		if(server == null) return null;
		//TODO post events
		final ConfigureConnectionEvent event = new ConfigureConnectionEvent(server, source.getLocalID());
		server.getEventDispatcher().post(server.ConfigureConnection, event);
		if(event.isCancelled()) return null;
		
		final InternalNetworkConnection peer = new InternalNetworkConnection(server, source,
				server.getConfig().getConnectionCheckTimeout(), true, event.getCustomObject());
		
		final boolean canConnect = server.addInitializedConnection(peer);
		if(!canConnect) {
			LOGGER.warning("Server %s rejected an internal connection, closing peer");
			peer.closeConnection();
			return null;
		}
		LOGGER.info("Created local peer connection between %s and %s", source.getLocalID(), source.getRemoteID());
		return peer;
	}
	
	protected static synchronized boolean register(NetworkManagerServer server) {
		final NetworkID serverId = server.getLocalID();
		if(serverList.containsKey(serverId)) {
			LOGGER.warning("Cannot register an internal server for the same local ID twice");
			return false;
		} else {
			serverList.put(serverId, server);
			LOGGER.info("Registered internal server for local ID %s", serverId.getDescription());
			return true;
		}
	}

	protected static synchronized boolean unregister(NetworkManagerServer server) {
		final NetworkID serverId = server.getLocalID();
		if(serverList.containsKey(serverId)) {
			serverList.remove(serverId);
			LOGGER.info("Unregistered internal server for loacl ID %s", serverId.getDescription());
			return true;
		} else {
			LOGGER.warning("Cannot unregister an internal server that is not in the local server list");
			return false;
		}
	}
	
	protected static synchronized Stream<NetworkManagerServer> getInternalServers() {
		return serverList.values().stream();
	}
}
