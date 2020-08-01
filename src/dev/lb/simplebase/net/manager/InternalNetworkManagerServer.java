package dev.lb.simplebase.net.manager;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.NetworkManagerServer;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.id.NetworkID;

public class InternalNetworkManagerServer extends NetworkManagerServer {

	protected InternalNetworkManagerServer(NetworkID local, ServerConfig config) {
		super(local, config);
		if(!config.getRegisterInternalServer()) 
			throw new IllegalArgumentException("Invalid ServerConfig: For serverType INTERNAL, registerInternalServer flag must be true");
	}

	@Override
	protected ServerManagerState startServerImpl() {
		NetworkManager.InternalAccess.INSTANCE.registerServerManagerForInternalConnections(this);
		return ServerManagerState.RUNNING;
	}

	@Override
	protected void stopServerImpl() {
		NetworkManager.InternalAccess.INSTANCE.unregisterServerManagerForInternalConnections(this);
	}
	
}
