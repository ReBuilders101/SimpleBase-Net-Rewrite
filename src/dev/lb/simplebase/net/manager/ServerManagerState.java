package dev.lb.simplebase.net.manager;

/**
 * Stroes the state of a {@link NetworkManagerServer} in its lifecycle.
 */
public enum ServerManagerState {

	/**
	 * The initial state of any server when it is created.
	 * The server cannot accept connections in this state.s
	 * <p>
	 * Can be switched to {@link #STARTING} by calling {@link NetworkManagerServer#startServer()}
	 * </p>
	 */
	INITIALIZED,
	/**
	 * The state of a server that is in the process of starting.
	 * <p>
	 * When starting is done, the state will automatically switch to {@link #RUNNING}.<br>
	 * If an error causes the starting process to fail, the state will switch to {@link #STOPPED} instead.
	 * </p>
	 */
	STARTING,
	/**
	 * The state of a server that is currently running and accepting connections.
	 * <p>
	 * The state can be switched to {@link #STOPPING} by calling {@link NetworkManagerServer#stopServer()}.
	 * </p>
	 */
	RUNNING,
	/**
	 * The state of a server that is in the process of stopping.
	 * <p>
	 * When stopiing is done, the state will automatically switch to {@link #STOPPED}.
	 * </p>
	 */
	STOPPING,
	/**
	 * The state of a server that is stopped.
	 * <p>
	 * A stooped server cannot accept connections and cannot restart. The state will never change
	 * again after it has been set to {@link #STOPPED}.
	 * </p>
	 */
	STOPPED;
	
}
