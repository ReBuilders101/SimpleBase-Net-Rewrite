package dev.lb.simplebase.net.manager;

import java.net.ServerSocket;
import java.net.Socket;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.manager.ServerSocketAcceptorThread.AcceptorThreadDeathReason;

public class SocketNetworkManagerServer extends NetworkManagerServer {

	private final TcpModule tcpModule;
	private final UdpModule udpModule;
	private final LanModule lanModule;
	
	protected SocketNetworkManagerServer(NetworkID local, ServerConfig config) {
		super(local, config);
		
		//Module setup
		final boolean detection = config.getAllowDetection();
		if(config.getServerType().supportsUdp()) {
			udpModule = new UdpModule(detection);
			lanModule = detection ? udpModule : null;
		} else {
			udpModule = null;
			lanModule = detection ? new LanModule() : null;
		}
		if(config.getServerType().supportsTcp()) {
			tcpModule = new TcpModule();
		} else {
			tcpModule = null;
		}
	}
	
	
	public boolean supportsUdp() {
		return udpModule != null;
	}
	
	public boolean supportsTcp() {
		return tcpModule != null;
	}
	
	public boolean supportsDetection() {
		return lanModule != null;
	}

	@Override
	protected ServerManagerState startServerImpl() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected void stopServerImpl() {
		// TODO Auto-generated method stub
		
	}
	
	@Internal
	void notifyAcceptorThreadClosure(AcceptorThreadDeathReason reason) {
		if(supportsTcp()) {
			tcpModule.notifyAcceptorThreadClosure(reason);
		} else {
			LOGGER.warning("SocketNetworkManagerServer was notified of acceptor thread death despite not managing a TCP module");
		}
	}
	
	@Internal
	void acceptIncomingRawConnection(Socket connectedSocket) {
		
	}
	
	
	private final class TcpModule {
		private final ServerSocket serverSocket;
		
		public void notifyAcceptorThreadClosure(AcceptorThreadDeathReason reason) {
			
		}
		
	}
	
	private final class UdpModule extends LanModule {
		public UdpModule(boolean lanCapable) {
			
		}
		
	}
	
	private class LanModule {
		
	}
}
