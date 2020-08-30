package dev.lb.simplebase.net.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.connection.ExternalNetworkConnection;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.event.EventAccessor;
import dev.lb.simplebase.net.events.ConfigureConnectionEvent;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.events.FilterRawConnectionEvent;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.util.LockBasedThreadsafeIterable;
import dev.lb.simplebase.net.util.LockHelper;
import dev.lb.simplebase.net.util.Task;
import dev.lb.simplebase.net.util.ThreadsafeIterable;

@Threadsafe
public abstract class NetworkManagerServer extends NetworkManagerCommon {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("server-manager");
	
	private final Map<NetworkID, NetworkConnection> connections;
	protected volatile ServerManagerState currentState;
	
	private final ReadWriteLock lockServer;
	private final ThreadsafeIterable<NetworkManagerServer, NetworkConnection> readThreadsafe;
	private final ThreadsafeIterable<NetworkManagerServer, NetworkConnection> writeThreadsafe;
	
	/**
	 * The {@link ConfigureConnectionEvent} will be posted when a new connection has been accepted by the server.<br>
	 * Can be used to set the connection's custom object.
	 */
	public final EventAccessor<ConfigureConnectionEvent> ConfigureConnection = new EventAccessor<>(ConfigureConnectionEvent.class);
	
	/**
	 * The {@link FilterRawConnectionEvent} will be posted when a new connection from a network location is attempted.
	 * Can be used to decline/terminate a connection based in IP-Address before a NetworkId is created.
	 * <br>Can be used to set the string name of the used {@link NetworkID}.
	 * <p>
	 * If cancelled, the connection will be terminated.
	 * <p>
	 * <b>Not fired for internal connections</b><br>
	 * Use this for filtering only, use the {@link #ConfigureConnection} event to react to a new connection.
	 */
	public final EventAccessor<FilterRawConnectionEvent> FilterRawConnection = new EventAccessor<>(FilterRawConnectionEvent.class);
	
	protected NetworkManagerServer(NetworkID local, ServerConfig config) {
		super(local, config);
		this.connections = new HashMap<>();
		this.lockServer = new ReentrantReadWriteLock();
		this.currentState = ServerManagerState.INITIALIZED;
		
		this.readThreadsafe = new LockBasedThreadsafeIterable<>(this, connections::values, lockServer.readLock(), true);
		this.writeThreadsafe = new LockBasedThreadsafeIterable<>(this, connections::values, lockServer.writeLock(), true);
	}

	public ThreadsafeIterable<NetworkManagerServer, NetworkConnection> readOnlyThreadsafe() {
		return readThreadsafe;
	}
	
	public ThreadsafeIterable<NetworkManagerServer, NetworkConnection> exclusiveThreadsafe() {
		return writeThreadsafe;
	}
	
	/**
	 * Lists all currently prensent active connections.
	 * Creates a mutable <b>copy</b> of the current connection list.
	 * The list will no longer be modified and can safely be stored.
	 * @return A list of all currently present connections
	 * @see #getConnectionsFast()
	 */
	public List<NetworkConnection> getConnectionsCopy() {
		try {
			lockServer.readLock().lock();
			return new ArrayList<>(connections.values());
		} finally {
			lockServer.readLock().unlock();
		}
	}
	
	/**
	 * Lists all currently prensent active connections
	 * This method avoids making a copy of the connections list for quick read-only access.
	 * <p>
	 * <b>May only be called from threadsafe code</b><br>
	 * Use {@link #readOnlyThreadsafe()} or {@link #exclusiveThreadsafe()} and 
	 * {@link ThreadsafeIterable#action(Consumer)} to acquire a lock for the connections list
	 * @return A stream of all currently present connections
	 * @throws IllegalStateException If the lock is not held by the current thread (optional)
	 * @see #getConnectionsCopy()
	 */
	public Iterable<NetworkConnection> getConnectionsFast() {
		if(LockHelper.isHeldByCurrentThread(lockServer.readLock(), true)) { 
			return connections.values();
		} else {
			throw new IllegalStateException("Current thread does not hold lock"); //No lock, no iterator
		}
	}
	
	public Task startServer() {
		try {
			lockServer.writeLock().lock();
			if(currentState == ServerManagerState.INITIALIZED) {
				LOGGER.info("Starting server (%s)...", getLocalID().getDescription());
				currentState = ServerManagerState.STARTING;
				if(getConfig().getRegisterInternalServer())
					NetworkManager.InternalAccess.INSTANCE.registerServerManagerForInternalConnections(this);
				//Custom startup
				return startServerImpl();
			} else {
				LOGGER.error("Cannot start server (%s) from state %s", getLocalID().getDescription(), currentState);
			}
		} finally {
			lockServer.writeLock().unlock();
		}
		return Task.completed();
	}
	
	/**
	 * State will be synced.<br>
	 * State will still be STARTING<br>
	 * Starting server %s... already logged
	 * @return The resulting state of starting
	 */
	@Internal
	protected abstract Task startServerImpl();
	
	public Task stopServer() {
		try {
			lockServer.writeLock().lock();
			if(currentState == ServerManagerState.RUNNING) {
				LOGGER.info("Stopping server (%s)...", getLocalID().getDescription());
				currentState = ServerManagerState.STOPPING;
				if(getConfig().getRegisterInternalServer())
					NetworkManager.InternalAccess.INSTANCE.unregisterServerManagerForInternalConnections(this);
				//Disconnect everyone
				LOGGER.info("Closing %d connections for server shutdown", connections.size());
				for(NetworkConnection con : connections.values()) {
					con.closeConnection(ConnectionCloseReason.SERVER);
				}
				//remove all closed connections
				connections.clear();
				//Then custom stuff
				return stopServerImpl();
			} else if(currentState == ServerManagerState.STOPPED) {
				LOGGER.info("Server (%s) is already stopped", getLocalID().getDescription());
			} else {
				LOGGER.error("Cannot stop server (%s) from state %s", getLocalID().getDescription(), currentState);
			}
		} finally {
			lockServer.writeLock().unlock();
		}
		return Task.completed();
	}
	
	/**
	 * Will be synced on state, state is STOPPING.
	 * Clients will be disconnected afterwards.
	 * Will always go to state STOPPED.
	 */
	protected abstract Task stopServerImpl();
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServerConfig getConfig() {
		return (ServerConfig) super.getConfig();
	}
	
	/**
	 * The current state of the server. As the state might be modified
	 * concurrently, the returned value is outdated immediately.
	 * It is recommended to use {@link #readOnlyThreadsafe()} if the state should not be changed
	 * while a block of code is running.
	 * @return The current {@link ServerManagerState}
	 */
	public ServerManagerState getCurrentState() {
		return currentState; //Volatile single-get -> no sync
	}
	
	/**
	 * Finds a network connection for a remote side NetworkID. Returns {@code null} if no connection was found.
	 * <p>
	 * This method serves both as a check (as in a hypothetical {@code hasConnection()} method and as a getter
	 * method at the same time to improve thread safety.
	 * Both checking for connections and retrieving the connection are bundled into one effectively
	 * atomic operation through this method. It is recommended to only call this method once in a logical
	 * block of code that deals with the connection, and to store the result in a local variable.
	 * @param remoteID The remote {@link NetworkID} of the connection that should be found
	 * @return The {@link NetworkConnection} object that represents the connection from this manager
	 * to the remote {@link NetworkID} in the parameter, or {@code null} if a connection to the 
	 * desired remote ID does (no longer) exist.
	 */
	public NetworkConnection getConnection(NetworkID remoteID) {
		try {
			lockServer.readLock().lock();
			return connections.get(remoteID);
		} finally {
			lockServer.readLock().unlock();
		}
	}
	
	
	public boolean sendPacketToClient(NetworkID client, Packet packet) {
		try {
			lockServer.readLock().lock();
			final NetworkConnection con = connections.get(client);
			if(con == null) {
				return false;
			} else {
				return con.sendPacket(packet);
			}
		} finally {
			lockServer.readLock().unlock();
		}
	}
	
	public boolean sendPacketToAllClients(Packet packet) {
		try {
			lockServer.readLock().lock();
			boolean allSent = true;
			for(NetworkConnection con : connections.values()) {
				allSent &= con.sendPacket(packet);
			}
			return allSent;
		} finally {
			lockServer.readLock().unlock();
		}
	}

	/**
	 * The connection will simply be added, the connection creator is responsible for posting the correct events
	 * @param newConnection The new connection
	 * @return Whether the connection was successful
	 */
	@Internal
	public boolean addInitializedConnection(NetworkConnection newConnection) {
		if(currentState != ServerManagerState.RUNNING) return false;
		try {
			lockServer.writeLock().lock();
			if(currentState != ServerManagerState.RUNNING) return false;
			final NetworkID id = newConnection.getRemoteID();
			if(connections.get(id) == null) {
				connections.put(id, newConnection);
			} else {
				throw new IllegalArgumentException("Connection with that ID is already present");
			}
			if(newConnection instanceof ExternalNetworkConnection) { //A bit of a patchwork, but ok
				((ExternalNetworkConnection) newConnection).sendConnectionAcceptedMessage();
			}
			return true;
		} finally {
			lockServer.writeLock().unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	protected <T extends NetworkConnection> T getConnectionImplementation(Class<T> type, Predicate<NetworkID> condition) {
		try {
			lockServer.readLock().lock();
			//Already locked, so use the fast non-copying getter
			for(NetworkConnection connection : getConnectionsFast()) {
				if(type.isInstance(connection) && condition.test(connection.getRemoteID()))
					return (T) connection;
			}
			//Nothing found
			return null;
		} finally {
			lockServer.readLock().unlock();
		}
	}

	@Override
	@Internal
	public void removeConnectionSilently(NetworkConnection connection) {
		try {
			lockServer.writeLock().lock();
			final NetworkID id = connection.getRemoteID();
			final NetworkConnection removedCon = connections.remove(id);
			if(removedCon != connection) throw new IllegalStateException("Server connection map was in an invalid state");
		} finally {
			lockServer.writeLock().unlock();
		}
	}

	public boolean disconnectClient(NetworkID clientId) {
		try {
			lockServer.writeLock().lock();
			final NetworkConnection removedCon = connections.get(clientId);
			if(removedCon == null) return false;
			removedCon.closeConnection();
			return true;
		} finally {
			lockServer.writeLock().unlock();
		}
	}

	@Override
	public EventAccessor<?>[] getEvents() {
		return new EventAccessor<?>[] {
			ConnectionClosed,
			PacketSendingFailed,
			PacketReceiveRejected,
			ConfigureConnection
		};
	}

	@Override
	public void updateConnectionStatus() {
		try {
			lockServer.writeLock().lock();
			for(NetworkConnection con : connections.values()) {
				con.updateConnectionStatus();
			}
		} finally {
			lockServer.writeLock().unlock();
		}
	}

	@Override
	public void cleanUp() {
		super.cleanUp();
		if(currentState != ServerManagerState.STOPPED) {
			stopServer();
		}
	}
}
