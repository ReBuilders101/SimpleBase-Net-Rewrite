package dev.lb.simplebase.net.manager;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.config.ServerType;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.util.Task;

public class InternalNetworkManagerServer extends NetworkManagerServer {

	protected InternalNetworkManagerServer(NetworkID local, ServerConfig config) {
		super(local, config);
		if(ServerType.resolve(config.getServerType(), local) != ServerType.INTERNAL)
			throw new IllegalArgumentException("Invalid ServerConfig: ServerType must be INTERNAL");
		if(!config.getRegisterInternalServer()) 
			throw new IllegalArgumentException("Invalid ServerConfig: For serverType INTERNAL, registerInternalServer flag must be true");
	}

	@Override
	protected Task startServerImpl() {
		NetworkManager.InternalAccess.INSTANCE.registerServerManagerForInternalConnections(this);
		currentState = ServerManagerState.RUNNING;
		LOGGER.info("... Sever start successful (%s)", getLocalID().getDescription());
		return Task.completed();
	}

	@Override
	protected Task stopServerImpl() {
		NetworkManager.InternalAccess.INSTANCE.unregisterServerManagerForInternalConnections(this);
		currentState = ServerManagerState.STOPPED;
		LOGGER.info("... Server stopped (%s)", getLocalID().getDescription());
		return Task.completed();
	}
	
}
