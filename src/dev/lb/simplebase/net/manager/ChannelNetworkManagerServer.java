package dev.lb.simplebase.net.manager;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.config.ServerType;
import dev.lb.simplebase.net.connection.ChannelConnection;
import dev.lb.simplebase.net.connection.TcpChannelNetworkConnection;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;

public class ChannelNetworkManagerServer extends NetworkManagerServer implements SelectorManager {

	private final SelectorThread selectorThread;
	private final TcpModule tcpModule;
	private final UdpModule udpModule;

	protected ChannelNetworkManagerServer(NetworkID local, ServerConfig config) throws IOException {
		super(local, config);
		final ServerType actualType = ServerType.resolve(config.getServerType(), local);
		if(!actualType.useSockets()) throw new IllegalArgumentException("Invalid ServerConfig: ServerType must use Sockets");

		this.selectorThread = new SelectorThread();
		
		//Module setup
		final boolean detection = config.getAllowDetection();
		final boolean udpConnect = actualType.supportsUdp();
		if(detection || udpConnect) {
			udpModule = new UdpModule(detection, udpConnect);
		} else {
			udpModule = null;
		}
		if(actualType.supportsTcp()) {
			tcpModule = new TcpModule();
		} else {
			tcpModule = null;
		}
	}

	@Override
	protected ServerManagerState startServerImpl() {
		if(tcpModule != null) {
			try {
				tcpModule.start();
			} catch (IOException e) {
				LOGGER.error("Cannot start ChannelNetworkManagerServer.TcpModule", e);
				return ServerManagerState.STOPPED;
			}
		}
		
		//TODO UdpModule
		
		selectorThread.start();
		return ServerManagerState.RUNNING;
	}

	@Override
	protected void stopServerImpl() {
		if(tcpModule != null) {
			try {
				tcpModule.stop();
			} catch (IOException e) {
				LOGGER.error("Cannot stop ChannelNetworkManagerServer.TcpModule", e);
			}
		}
		
		//TODO UdpModule
		
		LOGGER.info("... Server stopped (%s)", getLocalID().getDescription());
	}

	@Override
	public SelectionKey registerConnection(SocketChannel channel, int ops, ChannelConnection connection) {
		try {
			return channel.register(selectorThread.selector, ops, connection);
		} catch (ClosedChannelException e) {
			LOGGER.error("Cannot register channel with selector: Already closed", e);
			return null;
		}
	}
	
	@Internal
	void acceptIncomingRawTcpConnection(SocketChannel connectedChannel) {
		postIncomingTcpConnection(connectedChannel.socket(), (id, data) -> new TcpChannelNetworkConnection(this, this, id, connectedChannel));
	}

	private class TcpModule {
		private final ServerSocketChannel serverChannel;
		private SelectionKey selectionKey;
		
		public TcpModule() throws IOException {
			this.serverChannel = ServerSocketChannel.open();
			this.serverChannel.configureBlocking(false);
		}
		
		public void start() throws IOException {
			serverChannel.bind(getLocalID().getFunction(NetworkIDFunction.BIND));
			selectionKey = serverChannel.register(selectorThread.selector, SelectionKey.OP_ACCEPT);
		}
		
		public void stop() throws IOException {
			if(selectionKey != null) selectionKey.cancel();
			serverChannel.close();
		}
		
		public void acceptNow() {
			SocketChannel channel;
			try {
				channel = serverChannel.accept();
			} catch (IOException e) {
				LOGGER.error("Error while accepint incoming TCP connection", e);
				return;
			}
			if(channel == null) {
				LOGGER.warning("Server socket channel instructed to accept, but no connection was pending");
				return;
			}
			//Post all the events
			ChannelNetworkManagerServer.this.acceptIncomingRawTcpConnection(channel);
		}
		
	}

	private class UdpModule implements ChannelConnection {
		private final DatagramChannel channel;

		public UdpModule(boolean lan, boolean udp) throws IOException {
			this.channel = DatagramChannel.open();
			this.channel.configureBlocking(false);
			this.channel.register(selectorThread.selector, SelectionKey.OP_READ, this);
		}

		@Override
		public void readNow() {
			
		}
	}

	private static final AtomicInteger SELECTOR_THREAD_ID = new AtomicInteger();
	private class SelectorThread extends Thread {
		private final Selector selector;

		public SelectorThread() throws IOException {
			super("ServerSelectorHandlerThread-" + SELECTOR_THREAD_ID.getAndIncrement());
			this.selector = Selector.open();
		}

		@Override
		public void run() {
			try {
				while(!isInterrupted()) {
					int amount = selector.select(); //Interrupt cancels this
					//Handle all keys
					if(amount > 0) {
						Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
						while(keys.hasNext()) {
							SelectionKey current = keys.next();
							if(current.isReadable()) {
								ChannelConnection connection = (ChannelConnection) current.attachment();
								connection.readNow();
							}

							if(current.isAcceptable()) {
								if(tcpModule == null) {
									LOGGER.warning("TcpModule not present, but channel %s is listening for OP_ACCEPT", current.channel());
								} else {
									tcpModule.acceptNow();
								}
							}
							keys.remove(); //mark as handled
						}
					}

				}
			} catch (IOException e) {
				LOGGER.error("Error while selecting channels for processing", e);
			} finally {
				LOGGER.info("Selector handler thread stopped.");
			}
		}

	}
}
