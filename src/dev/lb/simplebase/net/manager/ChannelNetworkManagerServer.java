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
import dev.lb.simplebase.net.connection.TcpChannelNetworkConnection;
import dev.lb.simplebase.net.connection.UdpServerChannelNetworkConnection;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.packet.converter.AddressBasedDecoderPool;
import dev.lb.simplebase.net.packet.converter.AnonymousServerConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.MutableAddressConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;
import dev.lb.simplebase.net.task.Task;

@Internal
public class ChannelNetworkManagerServer extends ExternalNetworkManagerServer implements SelectorManager {

	private final SelectorThread selectorThread;
	private final Selector selector;
	
	//NEW TCP//
	private final ServerSocketChannel tcp_serverChannel;
	
	//NEW UDP//
	private final DatagramChannel udp_serverChannel;
	private final ByteBuffer udp_receiveBuffer;
	private final AddressBasedDecoderPool udp_decoderPool;
	private final PacketToByteConverter udp_toByteConverter;
	
	@Internal
	public ChannelNetworkManagerServer(NetworkID local, ServerConfig config, int depth) throws IOException {
		super(local, config, false, depth + 1);
		final ServerType actualType = ServerType.resolve(config.getServerType(), local);
		if(actualType.useSockets()) throw new IllegalArgumentException("Invalid ServerConfig: ServerType must not use Sockets");

		
		this.selector = Selector.open();
		this.selectorThread = new SelectorThread(selector);
		
		if(hasTcp) {
			tcp_serverChannel = ServerSocketChannel.open();
			tcp_serverChannel.configureBlocking(false);
		} else {
			tcp_serverChannel = null;
		}
		
		if(hasUdp || hasLan) {
			udp_serverChannel = DatagramChannel.open();
			udp_serverChannel.configureBlocking(false);
			udp_receiveBuffer = ByteBuffer.allocate(config.getDatagramPacketMaxSize());
			udp_toByteConverter = createToByteConverter();
			udp_decoderPool = new AddressBasedDecoderPool(UdpAnonymousConnectionAdapter::new, this);
		} else {
			udp_serverChannel = null;
			udp_receiveBuffer = null;
			udp_toByteConverter = null;
			udp_decoderPool = null;
		}
	}

	private void startTcpImpl() throws IOException {
		tcp_serverChannel.bind(getLocalID().getFunction(NetworkIDFunction.BIND));
		tcp_serverChannel.register(selector, SelectionKey.OP_ACCEPT); //The key is stored on the channel
	}
	
	private void startUdpLanImpl() throws IOException {
		udp_serverChannel.bind(getLocalID().getFunction(NetworkIDFunction.BIND));
		registerConnection(udp_serverChannel, SelectionKey.OP_READ, this::readUdpDataNow);
	}
	
	@Override
	protected Task startServerImpl() {
		if(hasTcp) {
			try {
				startTcpImpl();
			} catch (IOException e) {
				LOGGER.error("Cannot start ChannelNetworkManagerServer TCP module", e);
				currentState = ServerManagerState.STOPPED;
				return Task.completed();
			}
		}
		
		if(hasUdp || hasLan) {
			try {
				startUdpLanImpl();
			} catch (IOException e) {
				LOGGER.error("Cannot start ChannelNetworkManagerServer UDP module", e);
				//Try to stop other components, but if it fails that doesn't matter either
				try {
					stopTcpImpl();
				} catch (IOException e1) {
					LOGGER.error("Unable to stop ChannelNetworkManagerServer TCP module after server start failed");
				}
				
				currentState = ServerManagerState.STOPPED;
				return Task.completed();
			}
		}
		
		selectorThread.start();
		LOGGER.info("...Server started");
		currentState = ServerManagerState.RUNNING;
		return Task.completed();
	}

	private void stopTcpImpl() throws IOException {
		SelectionKey key = tcp_serverChannel.keyFor(selector);
		if(key != null) key.cancel();
		tcp_serverChannel.close();
	}
	
	private void stopUdpLanImpl() throws IOException {
		SelectionKey key = udp_serverChannel.keyFor(selector);
		if(key != null) key.cancel();
		udp_serverChannel.close();
	}
	
	@Override
	protected Task stopServerImpl() {
		selectorThread.interrupt();
		try {
			selectorThread.selector.close();
		} catch (IOException e) {
			LOGGER.error("Cannot close selector", e);
		}
		
		if(hasTcp) {
			try {
				stopTcpImpl();
			} catch (IOException e) {
				LOGGER.error("Cannot stop ChannelNetworkManagerServer TCP module", e);
			}
		}
		
		if(hasUdp || hasLan) {
			try {
				stopUdpLanImpl();
			} catch (IOException e) {
				LOGGER.error("Cannot stop ChannelNetworkManagerServer UDP module", e);
			}
		}
		
		currentState = ServerManagerState.STOPPED;
		LOGGER.info("... Server stopped (%s)", getLocalID().getDescription());
		return Task.completed();
	}

	private void acceptTcpConnectionNow() {
		final SocketChannel channel;
		try {
			channel = tcp_serverChannel.accept();
		} catch (IOException e) {
			LOGGER.error("Error while accepting incoming TCP connection", e);
			return;
		}
		if(channel == null) {
			LOGGER.warning("Server socket channel instructed to accept, but no connection was pending");
			return;
		}
		//Post all the events
		ChannelNetworkManagerServer.this.acceptIncomingRawTcpConnection(channel.socket(), (id, data) -> 
			new TcpChannelNetworkConnection(this, this, id, channel, data));
	}
	
	private void readUdpDataNow() {
		udp_receiveBuffer.clear();
		try {
			final SocketAddress source = udp_serverChannel.receive(udp_receiveBuffer);
			if(source == null) {
				LOGGER.warning("No Datagram packet available when reading");
				return;
			}
			if(!(source instanceof InetSocketAddress)) {
				LOGGER.warning("Datagram channel source address is not an InetSocketAddress");
				return;
			}
			final InetSocketAddress address = (InetSocketAddress) source;
			udp_receiveBuffer.flip();
			
			//Who will decode???
			decideUdpDataDestination(address, udp_decoderPool, udp_receiveBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void sendDatagram(SocketAddress address, ByteBuffer buffer) throws IOException {
		udp_serverChannel.send(buffer, address);
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
	
	@Override
	protected void sendUdpLogout(SocketAddress remoteAddress) {
		sendRawUdpByteData(remoteAddress, udp_toByteConverter.convert(NetworkPacketFormats.LOGOUT, null));
	}
	
	private static final AtomicInteger SELECTOR_THREAD_ID = new AtomicInteger();
	private class SelectorThread extends Thread {
		private final Selector selector;

		public SelectorThread(Selector selector) throws IOException {
			super("ServerSelectorHandlerThread-" + SELECTOR_THREAD_ID.getAndIncrement());
			this.selector = selector;
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
								if(hasTcp) {
									acceptTcpConnectionNow();
								} else {
									LOGGER.warning("TCP module not enabled, but channel %s is listening for OP_ACCEPT", current.channel());
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
			if(hasUdp) {
				acceptIncomingRawUdpConnection(address, (id, data) ->
					new UdpServerChannelNetworkConnection(ChannelNetworkManagerServer.this, id, data)); 
			} else {
				LOGGER.debug("Received a UDP-Login for server %s, but UDP module is disabled", getLocalID().getDescription());
			}
		}

		@Override
		public void receiveServerInfoRequest() {
			ChannelNetworkManagerServer.this.receiveServerInfoRequest(address);
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
