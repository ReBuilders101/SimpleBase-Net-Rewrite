package dev.lb.simplebase.net.manager;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.config.ServerType;
import dev.lb.simplebase.net.id.NetworkID;

public class InternalNetworkManagerServer extends NetworkManagerServer {

	protected InternalNetworkManagerServer(NetworkID local, ServerConfig config) {
		super(local, config);
		if(ServerType.resolve(config.getServerType(), local) != ServerType.INTERNAL)
			throw new IllegalArgumentException("Invalid NetworkID: For serverType INTERNAL, the NetworkIdFunction INTERNAL must be implemented");
		if(!config.getRegisterInternalServer()) 
			throw new IllegalArgumentException("Invalid ServerConfig: For serverType INTERNAL, registerInternalServer flag must be true");
	}

	@Override
	protected ServerManagerState startServerImpl() {
		NetworkManager.InternalAccess.INSTANCE.registerServerManagerForInternalConnections(this);
		LOGGER.info("... Sever start successful (%s)", getLocalID().getDescription());
		return ServerManagerState.RUNNING;
	}

	@Override
	protected void stopServerImpl() {
		NetworkManager.InternalAccess.INSTANCE.unregisterServerManagerForInternalConnections(this);
		LOGGER.info("... Server stopped (%s)", getLocalID().getDescription());
	}
	
}
