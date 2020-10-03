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
import dev.lb.simplebase.net.util.InternalAccess;

/**
 * Lists all available internal servers
 */
@Internal
public final class InternalServerProvider {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("internal-servers");
	
	private static final Map<NetworkID, NetworkManagerServer> serverList = new HashMap<>();
	
	@Internal
	public static synchronized InternalNetworkConnection createInternalConnectionPeer(InternalNetworkConnection source) {
		InternalAccess.assertCaller(InternalNetworkConnection.class, 0, "Cannot call createServerPeer() directly");
		
		final NetworkManagerServer server = serverList.get(source.getRemoteID());
		if(server == null) return null;
		final ConfigureConnectionEvent event = new ConfigureConnectionEvent(server, source.getLocalID());
		server.getEventDispatcher().post(server.ConfigureConnection, event);
//		if(event.isCancelled()) return null; //Is no longer cancellable
		
		final InternalNetworkConnection peer = new InternalNetworkConnection(server, source, event.getCustomObject());
		
		final boolean canConnect = server.addInitializedConnection(peer);
		if(!canConnect) {
			LOGGER.warning("Server %s rejected an internal connection, closing peer");
			peer.closeConnection();
			return null;
		}
		LOGGER.info("Created local peer connection between %s and %s", source.getLocalID(), source.getRemoteID());
		return peer;
	}
	
	@Internal
	public static synchronized boolean registerServerForInternalConnections(NetworkManagerServer server) {
		InternalAccess.assertCaller(NetworkManagerServer.class, 0, "Cannot call registerServerForInternalConnections() directly");
		
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

	@Internal
	public static synchronized boolean unregisterServerForInternalConnections(NetworkManagerServer server) {
		InternalAccess.assertCaller(NetworkManagerServer.class, 0, "Cannot call unregisterServerForInternalConnections() directly");
		
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
	
	protected static synchronized NetworkManagerServer getServer(NetworkID serverId) {
		if(serverList.containsKey(serverId)) {
			return serverList.get(serverId);
		} else {
			return null;
		}
	}
	
	protected static synchronized Stream<NetworkManagerServer> getInternalServers() {
		return serverList.values().stream();
	}
}
