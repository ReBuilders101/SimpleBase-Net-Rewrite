package dev.lb.simplebase.net.manager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.config.ServerType;
import dev.lb.simplebase.net.connection.DatagramSocketReceiverThread;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.connection.TcpSocketNetworkConnection;
import dev.lb.simplebase.net.connection.UdpServerSocketNetworkConnection;
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

public class SocketNetworkManagerServer extends NetworkManagerServer {

	private final TcpModule tcpModule;
	private final UdpModule udpModule;

	protected SocketNetworkManagerServer(NetworkID local, ServerConfig config) throws IOException {
		super(local, config);
		final ServerType actualType = ServerType.resolve(config.getServerType(), local);
		if(!actualType.useSockets()) throw new IllegalArgumentException("Invalid ServerConfig: ServerType must use Sockets");

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


	public boolean supportsUdp() {
		return udpModule != null && udpModule.udp;
	}

	public boolean supportsTcp() {
		return tcpModule != null;
	}

	public boolean supportsDetection() {
		return udpModule != null && udpModule.lan;
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

		if(udpModule != null) {
			try {
				udpModule.start();
			} catch (SocketException e) {
				LOGGER.error("Cannot start SocketNetworkManagerServer.UdpModule", e);
				//Also disable the tcp module that was started before
				if(tcpModule != null) tcpModule.stop();
				return ServerManagerState.STOPPED;
			}
		}

		LOGGER.info("...Server started");
		return ServerManagerState.RUNNING;
	}


	@Override
	protected void stopServerImpl() {
		if(tcpModule != null) {
			tcpModule.stop();
		}

		if(udpModule != null) {
			udpModule.stop();
		}
		LOGGER.info("... Server stopped (%s)", getLocalID().getDescription());
	}

	@Internal
	void notifyTCPAcceptorThreadClosure(AcceptorThreadDeathReason reason) {
		if(tcpModule != null) {
			tcpModule.notifyAcceptorThreadDeath(reason);
		} else {
			LOGGER.warning("SocketNetworkManagerServer was notified of acceptor thread death despite not managing a TCP module");
		}
	}

	void acceptIncomingRawUdpConnection(InetSocketAddress remoteAddress) {
		LOGGER.debug("Handling incoming UDP connection");
		final ServerManagerState stateSnapshot = getCurrentState(); //We are not synced here, but if it is STOPPING or STOPPED it can never be RUNNING again
		if(stateSnapshot.ordinal() > ServerManagerState.RUNNING.ordinal()) {
			LOGGER.warning("Declining incoming UDP socket connection because server is already %s", stateSnapshot);
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

			final NetworkConnection udpConnection = new UdpServerSocketNetworkConnection(this,
					networkId, event2.getCustomObject());

			//This will start the sync. An exclusive lock for this whole method would be too expensive
			if(!addInitializedConnection(udpConnection)) {
				//Can't connect after all, maybe the server was stopped in the time we created the connection
				LOGGER.warning("Re-Closed an initialized connection: Server was stopped during connection init");
				udpConnection.closeConnection();
			}
		}
	}
	
	@Internal
	void acceptIncomingRawTcpConnection(Socket connectedSocket) {
		postIncomingTcpConnection(connectedSocket, (id, data) -> new TcpSocketNetworkConnection(this, id, connectedSocket, data));
	}

	public void sendRawUdpByteData(SocketAddress address, ByteBuffer buffer) {
		if(udpModule != null) {
			udpModule.sendRawByteData(address, buffer);
		} else {
			LOGGER.warning("Cannot send raw UDP byte data: No UdpModule");
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
			serverSocket.bind(getLocalID().getFunction(NetworkIDFunction.BIND)); //Exception Here -> thread not yet started
			acceptorThread.start();
		}

		public void stop() {
			//this also closes the socket
			acceptorThread.interrupt();
		}

		public void notifyAcceptorThreadDeath(AcceptorThreadDeathReason reason) {
			LOGGER.debug("Ignoring TCP thread death notification %s: No cleanup required, server keeps running for other modules", reason);
		}

	}

	private class UdpModule {
		protected final DatagramSocket serverSocket;
		protected final AddressBasedDecoderPool pooledDecoders;
		protected final DatagramSocketReceiverThread receiverThread;
		protected final PacketToByteConverter toByteConverter;

		private final boolean lan;
		private final boolean udp;

		public UdpModule(boolean lan, boolean udp) throws SocketException {
			if(!(lan || udp)) throw new IllegalArgumentException("UdpModule must support either LAN or UDP connections");
			this.lan = lan;
			this.udp = udp;

			this.serverSocket = new DatagramSocket(null);
			this.pooledDecoders = new AddressBasedDecoderPool(UdpAnonymousConnectionAdapter::new, getMappingContainer(),
					getConfig().getPacketBufferInitialSize());
			this.receiverThread = new DatagramSocketReceiverThread(serverSocket, this::receiveRawByteData,
					this::notifyAcceptorThreadDeath, getConfig().getPacketBufferInitialSize());
			this.toByteConverter = lan ? new PacketToByteConverter(getMappingContainer(), null, getConfig().getPacketBufferInitialSize()) : null;
		}

		public void notifyAcceptorThreadDeath(AcceptorThreadDeathReason reason) {
			LOGGER.debug("Ignoring UDP thread death notification %s: No cleanup required, server keeps running for other modules", reason);
		}

		public void start() throws SocketException {
			serverSocket.bind(getLocalID().getFunction(NetworkIDFunction.BIND));
			receiverThread.start();
		}

		public void stop() {
			receiverThread.interrupt();
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
				Packet serverInfoPacket = getConfig().createServerInfoPacket(SocketNetworkManagerServer.this, address);
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
			final byte[] array = new byte[buffer.remaining()];
			buffer.get(array);
			try {
				serverSocket.send(new DatagramPacket(array, array.length, address));
			} catch (IOException e) {
				LOGGER.warning("Cannot send raw byte message with UDP socket", e);
			}
		}

		public void receiveRawByteData(InetSocketAddress address, ByteBuffer buffer) {
			final UdpServerSocketNetworkConnection connection = getUdpConnectionObject(address);
			if(connection != null) { //Yes, this is not locked: Exclusive locks are too expensive for any packet
				connection.decode(buffer);
			} else {
				pooledDecoders.decode(address, buffer);
			}
		}

		private UdpServerSocketNetworkConnection getUdpConnectionObject(InetSocketAddress address) {
			return readOnlyThreadsafe().actionReturn((server) -> {
				for(NetworkConnection connection : server.getConnectionsFast())
					if(connection instanceof UdpServerSocketNetworkConnection) {
						final NetworkID id = connection.getRemoteID();
						if(id.hasFunction(NetworkIDFunction.CONNECT)) {
							if(id.getFunction(NetworkIDFunction.CONNECT).equals(address)) {
								return (UdpServerSocketNetworkConnection) connection;
							}
						}
					}
				return null;
			});
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
