package dev.lb.simplebase.net.manager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
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
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.connection.TcpChannelNetworkConnection;
import dev.lb.simplebase.net.connection.UdpServerChannelNetworkConnection;
import dev.lb.simplebase.net.events.ConfigureConnectionEvent;
import dev.lb.simplebase.net.events.FilterRawConnectionEvent;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.converter.AddressBasedDecoderPool;
import dev.lb.simplebase.net.packet.converter.AnonymousServerConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.MutableAddressConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;

public class ChannelNetworkManagerServer extends NetworkManagerServer implements SelectorManager {

	private final SelectorThread selectorThread;
	private final TcpModule tcpModule;
	private final UdpModule udpModule;

	protected ChannelNetworkManagerServer(NetworkID local, ServerConfig config) throws IOException {
		super(local, config);
		final ServerType actualType = ServerType.resolve(config.getServerType(), local);
		if(actualType.useSockets()) throw new IllegalArgumentException("Invalid ServerConfig: ServerType must not use Sockets");

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
		
		if(udpModule != null) {
			try {
				udpModule.start();
			} catch (IOException e) {
				LOGGER.error("Cannot start ChannelNetworkManagerServer.UdpModule", e);
				
				try {
					tcpModule.stop();
				} catch (IOException e1) {
					LOGGER.error("Unable to stop ChannelNetworkManagerServer.TcpModule after server start failed");
				}
				
				return ServerManagerState.STOPPED;
			}
		}
		
		selectorThread.start();
		LOGGER.info("...Server started");
		return ServerManagerState.RUNNING;
	}

	@Override
	protected void stopServerImpl() {
		selectorThread.interrupt();
		try {
			selectorThread.selector.close();
		} catch (IOException e) {
			LOGGER.error("Cannot close selector", e);
		}
		
		if(tcpModule != null) {
			try {
				tcpModule.stop();
			} catch (IOException e) {
				LOGGER.error("Cannot stop ChannelNetworkManagerServer.TcpModule", e);
			}
		}
		
		if(udpModule != null) {
			try {
				udpModule.stop();
			} catch (IOException e) {
				LOGGER.error("Cannot stop ChannelNetworkManagerServer.UdpModule", e);
			}
		}
		
		LOGGER.info("... Server stopped (%s)", getLocalID().getDescription());
	}

	public void sendRawUdpByteData(SocketAddress address, ByteBuffer buffer) {
		if(udpModule != null) {
			udpModule.sendRawByteData(address, buffer);
		} else {
			LOGGER.warning("Cannot send raw UDP byte data: No UdpModule");
		}
	}
	
	@Override
	public SelectionKey registerConnection(SelectableChannel channel, int ops, ChannelConnection connection) {
		try {
			return channel.register(selectorThread.selector, ops, connection);
		} catch (ClosedChannelException e) {
			LOGGER.error("Cannot register channel with selector: Already closed", e);
			return null;
		}
	}
	
	@Internal
	void acceptIncomingRawTcpConnection(SocketChannel connectedChannel) {
		postIncomingTcpConnection(connectedChannel.socket(), (id, data) -> new TcpChannelNetworkConnection(this, this, id, connectedChannel, data));
	}

	private void acceptIncomingRawUdpConnection(InetSocketAddress remoteAddress) {
		LOGGER.debug("Handling incoming UDP connection");
		final ServerManagerState stateSnapshot = getCurrentState(); //We are not synced here, but if it is STOPPING or STOPPED it can never be RUNNING again
		if(stateSnapshot.ordinal() > ServerManagerState.RUNNING.ordinal()) {
			LOGGER.warning("Declining incoming UDP channel connection because server is already %s", stateSnapshot);
			//Disconnect
			udpModule.sendRawByteData(remoteAddress, udpModule.toByteConverter.convert(NetworkPacketFormats.LOGOUT, null));
			return;
		}
		
		
		final FilterRawConnectionEvent event1 = new FilterRawConnectionEvent(remoteAddress, 
				ManagerInstanceProvider.generateNetworkIdName("RemoteId-"));
		getEventDispatcher().post(FilterRawConnection, event1);
		if(event1.isCancelled()) {
			//Disconnect
			udpModule.sendRawByteData(remoteAddress, udpModule.toByteConverter.convert(NetworkPacketFormats.LOGOUT, null));
		} else {
			final NetworkID networkId = NetworkID.createID(event1.getNetworkIdName(), remoteAddress);

			//Next event
			final ConfigureConnectionEvent event2 = new ConfigureConnectionEvent(this, networkId);
			getEventDispatcher().post(ConfigureConnection, event2);

			final NetworkConnection udpConnection = new UdpServerChannelNetworkConnection(this,
					networkId, event2.getCustomObject());

			//This will start the sync. An exclusive lock for this whole method would be too expensive
			if(!addInitializedConnection(udpConnection)) {
				//Can't connect after all, maybe the server was stopped in the time we created the connection
				LOGGER.warning("Re-Closed an initialized connection: Server was stopped during connection init");
				udpConnection.closeConnection();
			}
		}
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
		private final ByteBuffer receiveBuffer;
		private final AddressBasedDecoderPool pooledDecoders;
		private final PacketToByteConverter toByteConverter;
		private SelectionKey selectionKey;
		
		private final boolean lan;
		private final boolean udp;
		
		public UdpModule(boolean lan, boolean udp) throws IOException {
			this.lan = lan;
			this.udp = udp;
			this.channel = DatagramChannel.open();
			this.receiveBuffer = ByteBuffer.allocate(getConfig().getPacketBufferInitialSize());
			this.channel.configureBlocking(false);
			this.toByteConverter = lan ? new PacketToByteConverter(getMappingContainer(), null, getConfig().getPacketBufferInitialSize()) : null;
			this.pooledDecoders = new AddressBasedDecoderPool(UdpAnonymousConnectionAdapter::new,
					getMappingContainer(), getConfig().getPacketBufferInitialSize());
//			this.channel.register(selectorThread.selector, SelectionKey.OP_READ, this);
		}

		public void start() throws IOException {
			channel.bind(getLocalID().getFunction(NetworkIDFunction.BIND));
			selectionKey = registerConnection(channel, SelectionKey.OP_READ, this);
		}
		
		public void stop() throws IOException {
			if(selectionKey != null) selectionKey.cancel();
			channel.close();
		}

		@Override
		public void readNow() {
			receiveBuffer.clear();
			try {
				SocketAddress source = channel.receive(receiveBuffer);
				if(source == null) {
					LOGGER.warning("No Datagram packet available when reading");
					return;
				}
				if(!(source instanceof InetSocketAddress)) {
					LOGGER.warning("Datagram channel source address is not an InetSocketAddress");
					return;
				}
				final InetSocketAddress address = (InetSocketAddress) source;
				receiveBuffer.flip();
				
				//Who will decode???
				final UdpServerChannelNetworkConnection connection = getUdpConnectionObject(address);
				if(connection != null) { //Yes, this is not locked: Exclusive locks are too expensive for any packet
					connection.decode(receiveBuffer);
				} else {
					pooledDecoders.decode(address, receiveBuffer);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private UdpServerChannelNetworkConnection getUdpConnectionObject(InetSocketAddress address) {
			return readOnlyThreadsafe().actionReturn((server) -> {
				for(NetworkConnection connection : server.getConnectionsFast())
					if(connection instanceof UdpServerChannelNetworkConnection) {
						final NetworkID id = connection.getRemoteID();
						if(id.hasFunction(NetworkIDFunction.CONNECT)) {
							if(id.getFunction(NetworkIDFunction.CONNECT).equals(address)) {
								return (UdpServerChannelNetworkConnection) connection;
							}
						}
					}
				return null;
			});
		}
		
		public void receiveUdpLogin(InetSocketAddress address) {
			if(udp) {
				acceptIncomingRawUdpConnection(address);
			} else {
				LOGGER.debug("Received a UDP-Login for server %s, but UDP module is disabled", getLocalID().getDescription());
			}
		}

		public void receiveServerInfoRequest(InetSocketAddress address) {
			if(lan) {
				Packet serverInfoPacket = getConfig().createServerInfoPacket(ChannelNetworkManagerServer.this, address);
				if(serverInfoPacket == null) {
					LOGGER.debug("No server info reply packet could be generated (To: %s)", address);
				} else {
					sendRawByteData(address, toByteConverter.convert(NetworkPacketFormats.SERVERINFOAN, serverInfoPacket));
				}
			} else {
				LOGGER.debug("Received a Server-Info-Request for server %s, but LAN module is disabled", getLocalID().getDescription());
			}
		}
		
		public void sendRawByteData(SocketAddress address, ByteBuffer buffer) {
			try {
				channel.send(buffer, address);
			} catch (IOException e) {
				LOGGER.warning("Cannot send raw byte message with UDP socket", e);
			}
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
			LOGGER.info("Selector handler thread started");
			try {
				while(!isInterrupted()) {
					int amount = selector.select(); //Interrupt cancels this
					//Handle all keys
					if(amount > 0) {
						Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
						while(keys.hasNext()) {
							SelectionKey current = keys.next();
							if(current.isValid() && current.isReadable()) {
								ChannelConnection connection = (ChannelConnection) current.attachment();
								connection.readNow();
							}

							if(current.isValid() && current.isAcceptable()) {
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
	
	private class UdpAnonymousConnectionAdapter implements AnonymousServerConnectionAdapter, MutableAddressConnectionAdapter {

		private final ReferenceCounter counter;
		private volatile InetSocketAddress address;

		public UdpAnonymousConnectionAdapter(InetSocketAddress address) {
			this.address = address;
			this.counter = new ReferenceCounter();
		}

		@Override
		public void receiveUdpLogin() {
			udpModule.receiveUdpLogin(address);
		}

		@Override
		public void receiveServerInfoRequest() {
			udpModule.receiveServerInfoRequest(address);
		}

		@Override
		public void setAddress(InetSocketAddress address) {
			this.address = address;
		}

		@Override
		public ReferenceCounter getUseCountManager() {
			return counter;
		}

	}
}
