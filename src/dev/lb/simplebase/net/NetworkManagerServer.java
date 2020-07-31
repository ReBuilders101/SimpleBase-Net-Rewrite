package dev.lb.simplebase.net;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.event.EventAccessor;
import dev.lb.simplebase.net.events.ConfigureConnectionEvent;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.util.ThreadsafeAction;

@Threadsafe
public abstract class NetworkManagerServer extends NetworkManagerCommon implements ThreadsafeAction<NetworkManagerServer>{

	private final Map<NetworkID, NetworkConnection> connections;
	private final ReadWriteLock lockConnections;
	
	private ServerState currentState;
	private final Object lockCurrentState;
	
	/**
	 * The {@link ConfigureConnectionEvent} will be posted when a new connection has been accepted by the server.<br>
	 * Can be used to set the connection's custom object
	 */
	public final EventAccessor<ConfigureConnectionEvent> ConfigureNewConnection = new EventAccessor<>(ConfigureConnectionEvent.class);
	
	protected NetworkManagerServer(NetworkID local, ServerConfig config) {
		super(local, config);
		this.connections = new HashMap<>();
		this.lockConnections = new ReentrantReadWriteLock();
		
		this.currentState = ServerState.INITIALIZED;
		this.lockCurrentState = new Object();
	}

	
	public boolean startServer() {
		synchronized (lockCurrentState) {
			if(currentState != ServerState.INITIALIZED) {
				return false;
			} else {
				currentState = ServerState.STARTING;
				NetworkManager.NET_LOG.info("Attempting to start server with ID %s", getLocalID());
				
				if(startServerImpl()) {
					registerInternal();
				}
				return true;
			}
		}
	}
	
	/**
	 * Will be synced on state, state is STARTING.
	 * Has to set the resulting state and do the log itself
	 * @return whether it was successful at starting
	 */
	@Internal
	protected abstract boolean startServerImpl();
	
	/**
	 * registers this on the internal server manager
	 */
	@Internal
	private void registerInternal() {
		if(getConfig().getRegisterInternalServer()) {
			InternalServerManager.register(this);
		}
	}
	
	public boolean stopServer() {
		synchronized (lockCurrentState) {
			if(currentState != ServerState.RUNNING) {
				return false;
			} else {
				currentState = ServerState.STOPPING;
				NetworkManager.NET_LOG.info("Attempting to stop server with ID %s", getLocalID());
				
				if(getConfig().getRegisterInternalServer()) {
					InternalServerManager.unregister(this);
				}
				
				stopServerImpl();
				
				try {
					lockConnections.writeLock().lock();
					NetworkManager.NET_LOG.info("Closing %d connections for server shutdown", connections.size());
					for(NetworkConnection con : connections.values()) {
						con.closeConnection(ConnectionCloseReason.SERVER);
					}
				} finally {
					lockConnections.writeLock().unlock();
				}
				currentState = ServerState.STOPPED;
				NetworkManager.NET_LOG.info("Server stopped. (ID %s)", getLocalID());
				return true;
			}
		}
	}
	
	/**
	 * Will be synced on state, state is STOPPING.
	 * Clients will be disconnected afterwards.
	 * Will always go to state STOPPED.
	 */
	protected abstract void stopServerImpl();
	
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
	 * It is recommended to use {@link #getThreadsafeState()} together with
	 * {@link #action(java.util.function.Consumer)} if the state should not be changed
	 * while a block of code is running.
	 * @return The current {@link ServerState}
	 */
	public ServerState getCurrentState() {
		synchronized (lockCurrentState) {
			return currentState;
		}
	}
	
	/**
	 * The current {@link ServerState} of this connection.<p>
	 * This method can only be used if the caller thread holds the state's monitor.
	 * It is designed to be used inside a {@link #action(Consumer)} block when
	 * concurrent modification should be prevented.
	 * @return The current connection state
	 * @throws IllegalStateException If the current thread does not hold the state's monitor
	 */
	public ServerState getThreadsafeState() {
		if(Thread.holdsLock(lockCurrentState)) {
			return currentState;
		} else {
			throw new IllegalStateException("Current thread does not hold object monitor");
		}
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
			lockConnections.readLock().lock();
			return connections.get(remoteID);
		} finally {
			lockConnections.readLock().unlock();
		}
	}

	/**
	 * The connection will simply be added, the connection creator is responsible for posting the correct events
	 * @param newConnection The new connection
	 */
	@Internal
	protected void addInitializedConnection(NetworkConnection newConnection) {
		try {
			lockConnections.writeLock().lock();
			final NetworkID id = newConnection.getRemoteID();
			if(connections.get(id) == null) {
				connections.put(id, newConnection);
			} else {
				throw new IllegalArgumentException("Connection with that ID is already present");
			}
		} finally {
			lockConnections.writeLock().unlock();
		}
	}

	@Override
	protected PacketContext getConnectionlessPacketContext(NetworkID source) {
		try {
			lockConnections.readLock().lock();
			final NetworkConnection connection = connections.get(source);
			return connection == null ? null : connection.getContext();
		} finally {
			lockConnections.readLock().unlock();
		}
	}


	@Override
	protected void removeConnectionSilently(NetworkConnection connection) {
		try {
			lockConnections.writeLock().lock();
			final NetworkID id = connection.getRemoteID();
			final NetworkConnection removedCon = connections.remove(id);
			if(removedCon != connection) throw new IllegalStateException("Server connection map was in an invalid state");
		} finally {
			lockConnections.writeLock().unlock();
		}
	}

	@Override
	protected void onCheckConnectionStatus() {
		try {
			lockConnections.readLock().lock();
			for(NetworkConnection con : connections.values()) {
				con.updateConnectionStatus();
			}
		} finally {
			lockConnections.readLock().unlock();
		}
	}


	@Override
	public EventAccessor<?>[] getEvents() {
		return new EventAccessor<?>[] {
			ConnectionClosed,
			ConnectionCheckSuccess,
			PacketSendingFailed,
			PacketReceiveRejected,
			UnknownConnectionlessPacket,
			ConfigureNewConnection
		};
	}


	@Override
	public void action(Consumer<NetworkManagerServer> action) {
		try {
			lockConnections.writeLock().lock();
			action.accept(this);
		} finally {
			lockConnections.writeLock().unlock();
		}
	}


	@Override
	public <R> R actionReturn(Function<NetworkManagerServer, R> action) {
		try {
			lockConnections.writeLock().lock();
			return action.apply(this);
		} finally {
			lockConnections.writeLock().unlock();
		}
	}
	
}
