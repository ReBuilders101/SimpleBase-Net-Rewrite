package dev.lb.simplebase.net.manager;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.config.ServerType;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.task.Task;

@Internal
public final class InternalNetworkManagerServer extends NetworkManagerServer {

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
