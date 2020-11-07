package dev.lb.simplebase.net.manager;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.config.ServerType;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.task.Task;

/**
 * Internal implementation class of {@link NetworkManagerServer} that can only handle
 * application-internal connections and has not networking capabilities.
 * <p>
 * Can be created using {@link NetworkManager#createServer(NetworkID, ServerConfig)} when choosing
 * {@link ServerType#INTERNAL} or by enabling {@link ServerConfig#getRegisterInternalServer()}.
 */
@Internal
public final class InternalNetworkManagerServer extends NetworkManagerServer {

	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and can not be called directly.
	 * </p><hr><p>
	 * Creates a new {@link InternalNetworkManagerServer}.
	 * </p>
	 * @param local The local {@link NetworkID} of the server
	 * @param config The {@link ServerConfig} for the server
	 */
	@Internal
	public InternalNetworkManagerServer(NetworkID local, ServerConfig config) {
		super(local, config, 1);
		if(config.getServerType() != ServerType.INTERNAL)
			throw new IllegalArgumentException("Invalid ServerConfig: ServerType must be INTERNAL");
		if(!config.getRegisterInternalServer()) 
			throw new IllegalArgumentException("Invalid ServerConfig: For serverType INTERNAL, registerInternalServer flag must be true");
	}

	@Override
	protected Task startServerImpl() {
		currentState = ServerManagerState.RUNNING;
		LOGGER.info("... Sever start successful (%s)", getLocalID().getDescription());
		return Task.completed();
	}

	@Override
	protected Task stopServerImpl() {
		currentState = ServerManagerState.STOPPED;
		LOGGER.info("... Server stopped (%s)", getLocalID().getDescription());
		return Task.completed();
	}
	
}
