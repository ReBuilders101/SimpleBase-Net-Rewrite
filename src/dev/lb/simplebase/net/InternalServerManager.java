package dev.lb.simplebase.net;

import java.util.HashMap;
import java.util.Map;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.id.NetworkID;

/**
 * Lists all available internal servers
 */
@Internal
class InternalServerManager {

	private static final Map<NetworkID, NetworkManagerServer> serverList = new HashMap<>();
	
	protected static synchronized LocalPeerNetworkConnection createServerPeer(LocalPeerNetworkConnection source) {
		final NetworkManagerServer server = serverList.get(source.getRemoteID());
		if(server == null) return null;
		//TODO post events
		final LocalPeerNetworkConnection peer = new LocalPeerNetworkConnection(server.getLocalID(), source.getLocalID(),
				server, server.getConfig().getConnectionCheckTimeout(), true, null, source); //TODO custom object -> ConnectEvent
		server.addInitializedConnection(peer);
		NetworkManager.NET_LOG.info("Created local peer connection between %s and %s", source.getLocalID(), source.getRemoteID());
		return peer;
	}
	
	protected static synchronized boolean register(NetworkManagerServer server) {
		final NetworkID serverId = server.getLocalID();
		if(serverList.containsKey(serverId)) {
			NetworkManager.NET_LOG.warning("Cannot register an internal server for the same local ID twice");
			return false;
		} else {
			serverList.put(serverId, server);
			return true;
		}
	}

	protected static synchronized boolean unregister(NetworkManagerServer server) {
		final NetworkID serverId = server.getLocalID();
		if(serverList.containsKey(serverId)) {
			serverList.remove(serverId);
			return true;
		} else {
			NetworkManager.NET_LOG.warning("Cannot unregister an internal server that is not in the local server list");
			return false;
		}
	}
	
}
