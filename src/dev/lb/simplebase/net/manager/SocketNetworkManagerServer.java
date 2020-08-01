package dev.lb.simplebase.net.manager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.config.ServerType;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.events.ConfigureConnectionEvent;
import dev.lb.simplebase.net.events.FilterRawConnectionEvent;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.ServerSocketAcceptorThread.AcceptorThreadDeathReason;

public class SocketNetworkManagerServer extends NetworkManagerServer {
	
	private final TcpModule tcpModule;
	private final UdpModule udpModule;
	private final LanModule lanModule;
	
	protected SocketNetworkManagerServer(NetworkID local, ServerConfig config) throws IOException {
		super(local, config);
		final ServerType actualType = ServerType.resolve(config.getServerType(), local);
		if(!actualType.useSockets()) throw new IllegalArgumentException("Invalid ServerConfig: ServerType must use Sockets");
		
		//Module setup
		final boolean detection = config.getAllowDetection();
		if(actualType.supportsUdp()) {
			udpModule = new UdpModule(detection);
			lanModule = detection ? udpModule : null;
		} else {
			udpModule = null;
			lanModule = detection ? new LanModule() : null;
		}
		if(actualType.supportsTcp()) {
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
		if(tcpModule != null) {
			try {
				tcpModule.start();
			} catch (IOException e) {
				LOGGER.error("Cannot start SocketNetworkManagerServer.TcpModule", e);
				//Short-circuit return: No other components have been started yet
				return ServerManagerState.STOPPED;
			}
		}
		
		
		
		return ServerManagerState.RUNNING;
	}


	@Override
	protected void stopServerImpl() {
		if(tcpModule != null) {
			tcpModule.stop();
		}
	}
	
	@Internal
	void notifyAcceptorThreadClosure(AcceptorThreadDeathReason reason) {
		if(supportsTcp()) {
			tcpModule.notifyAcceptorThreadDeath(reason);
		} else {
			LOGGER.warning("SocketNetworkManagerServer was notified of acceptor thread death despite not managing a TCP module");
		}
	}
	
	@Internal
	void acceptIncomingRawConnection(Socket connectedSocket) {
		//Find the address depending on socket implementation
		final SocketAddress remote = connectedSocket.getRemoteSocketAddress();
		final InetSocketAddress remoteAddress;
		if(remote instanceof InetSocketAddress) {
			remoteAddress = (InetSocketAddress) remote;
		} else {
			remoteAddress = new InetSocketAddress(connectedSocket.getInetAddress(), connectedSocket.getPort());
		}
		
		//post and handle the event
		final FilterRawConnectionEvent event1 = new FilterRawConnectionEvent(remoteAddress, 
				ManagerInstanceProvider.generateNetworkIdName("RemoteId-"));
		getEventDispatcher().post(FilterRawConnection, event1);
		if(event1.isCancelled()) {
			try {
				connectedSocket.close();
			} catch (IOException e) {
				LOGGER.error("Error while closing a declined TCP socket", e);
			}
		} else {
			final NetworkID networkId = NetworkID.createID(event1.getNetworkIdName(), remoteAddress);
			
			//Next event
			final ConfigureConnectionEvent event2 = new ConfigureConnectionEvent(this, networkId);
			getEventDispatcher().post(ConfigureConnection, event2);
			
			event2.getCustomObject(); //use this for connection
			final NetworkConnection tcpConnection = null; //TODO impl
			addInitializedConnection(tcpConnection);
		}
	}
	
	
	private final class TcpModule {
		private final ServerSocket serverSocket;
		private final ServerSocketAcceptorThread acceptorThread;
		
		public TcpModule() throws IOException {
			this.serverSocket = new ServerSocket();
			this.acceptorThread = new ServerSocketAcceptorThread(SocketNetworkManagerServer.this, serverSocket);
		}
		
		public void start() throws IOException {
			final SocketAddress address = getLocalID().getFunction(NetworkIDFunction.BIND);
			serverSocket.bind(address); //Exception Here -> thread not yet started
			acceptorThread.start();
		}
		
		public void stop() {
			//this also closes the socket
			acceptorThread.interrupt();
		}
		
		@SuppressWarnings("unused")
		public boolean isActive() {
			return acceptorThread.isAlive() && serverSocket.isBound() && !serverSocket.isClosed();
		}
		
		public void notifyAcceptorThreadDeath(AcceptorThreadDeathReason reason) {
			LOGGER.debug("Ignoring thread death notification %s: No cleanup required, server keeps running for other modules", reason);
		}
		
	}
	
	private final class UdpModule extends LanModule {
		public UdpModule(boolean lanCapable) {
			
		}
		
	}
	
	private class LanModule {
		
	}
}
