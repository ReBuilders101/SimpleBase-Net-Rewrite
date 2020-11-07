package dev.lb.simplebase.net;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.connection.InternalNetworkConnection;
import dev.lb.simplebase.net.events.ConfigureConnectionEvent;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.Logger;
import dev.lb.simplebase.net.manager.NetworkManagerServer;
import dev.lb.simplebase.net.util.InternalAccess;

/**
 * <h2>Internal use only</h2>
 * <p>
 * This class is used internally by the API and the contained methods should not and can not be called directly.
 * </p><hr><p>
 * Manages the list of servers available for internal network connections. {@link NetworkManagerServer}s will automatically
 * register and unregister when starting and stopping if {@link ServerConfig#getRegisterInternalServer()} is enabled.
 * </p>
 * @see InternalAccess#assertCaller(Class, int, String)
 */
@Internal
public final class InternalServerProvider {
	static final Logger LOGGER = NetworkManager.getModuleLogger("internal-servers");
	
	private static final Map<NetworkID, NetworkManagerServer> serverList = new HashMap<>();
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and can not be called directly.
	 * </p><hr><p>
	 * Creates the peer that is stored in an {@link InternalNetworkConnection} when connecting the client to one of the
	 * registered internal servers.
	 * </p>
	 * @param source The client-side connection that wants to connect to a server
	 * @return The server-side connection, initialized with the source as its peer
	 * @see InternalAccess#assertCaller(Class, int, String)
	 */
	@Internal
	public static synchronized InternalNetworkConnection createInternalConnectionPeer(InternalNetworkConnection source) {
		InternalAccess.assertCaller(InternalNetworkConnection.class, 0, "Cannot call createServerPeer() directly");
		
		final NetworkManagerServer server = serverList.get(source.getRemoteID());
		if(server == null) return null;
		final ConfigureConnectionEvent event = new ConfigureConnectionEvent(server, source.getLocalID());
		server.getEventDispatcher().post(server.ConfigureConnection, event);
		
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
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and can not be called directly.
	 * </p><hr><p>
	 * Registers a {@link NetworkManagerServer} to be available for internal connections.
	 * Called automatically from {@link NetworkManagerServer#startServer()} if {@link ServerConfig#getRegisterInternalServer()}
	 * is enabled in the server config.
	 * </p><p>
	 * Registration will fail if the server is already registered.
	 * </p>
	 * @param server The server to register for internal connections
	 * @return Whether the registration was successful
	 * @see InternalAccess#assertCaller(Class, int, String)
	 */
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

	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and can not be called directly.
	 * </p><hr><p>
	 * Unregisters a {@link NetworkManagerServer} to no longer be available for internal connections.
	 * Called automatically from {@link NetworkManagerServer#stopServer()} if {@link ServerConfig#getRegisterInternalServer()}
	 * is enabled in the server config.
	 * </p><p>
	 * Unregistration will fail if the server not currently registered.
	 * </p>
	 * @param server The server to unregister for internal connections
	 * @return Whether the unregistration was successful
	 * @see InternalAccess#assertCaller(Class, int, String)
	 */
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
	
	/**
	 * Get a registered server for a certain {@link NetworkID}
	 * @param serverId The {@link NetworkID} for the server
	 * @return The registered server, or {@code null} of no matching server was found
	 */
	protected static synchronized NetworkManagerServer getServer(NetworkID serverId) {
		if(serverList.containsKey(serverId)) {
			return serverList.get(serverId);
		} else {
			return null;
		}
	}
	
	/**
	 * Lists all currently registered servers in a new immutable {@link Set}.
	 * @return A new {@link Set} containing all registered servers
	 */
	protected static synchronized Set<NetworkManagerServer> getInternalServers() {
		return Collections.unmodifiableSet(new HashSet<>(serverList.values()));
	}
}
