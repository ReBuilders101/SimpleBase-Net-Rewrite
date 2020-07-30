package dev.lb.simplebase.net;

import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.id.NetworkID;

public class InternalNetworkManagerServer extends NetworkManagerServer {

	protected InternalNetworkManagerServer(NetworkID local, ServerConfig config) {
		super(local, config);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean startServerImpl() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void stopServerImpl() {
		// TODO Auto-generated method stub
		
	}

}
