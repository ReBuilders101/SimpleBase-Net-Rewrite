package dev.lb.simplebase.net.manager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.connection.DatagramReceiverThread;
import dev.lb.simplebase.net.connection.TcpSocketNetworkConnection;
import dev.lb.simplebase.net.connection.UdpServerSocketNetworkConnection;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFeature;
import dev.lb.simplebase.net.log.Logger;
import dev.lb.simplebase.net.packet.converter.AddressBasedDecoderPool;
import dev.lb.simplebase.net.packet.converter.AnonymousServerConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.MutableAddressConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;
import dev.lb.simplebase.net.task.Task;

@Internal
public final class SocketNetworkManagerServer extends ExternalNetworkManagerServer {
	
	//NEW TCP SECTION//
	private final ServerSocket tcp_serverSocket;
	private final ServerSocketAcceptorThread tcp_acceptorThread; 
	
	//NEW UDP SECTION//
	private final DatagramSocket udp_serverSocket;
	private final DatagramReceiverThread udp_receiverThread;
	private final AddressBasedDecoderPool udp_decoderPool;
	private final PacketToByteConverter udp_toByteConverter;
	
	public static final BiPredicate<NetworkID, InetSocketAddress> matchRemoteAddress = (n, i) -> 
		n.ifFeature(NetworkIDFeature.CONNECT, r -> r.equals(i), false);
	
	@Internal
	@SuppressWarnings("deprecation")
	public SocketNetworkManagerServer(NetworkID local, ServerConfig config) throws IOException {
		super(local, config, true, 1);
		
		if(hasTcp) {
			tcp_serverSocket = new ServerSocket();
			tcp_acceptorThread = new ServerSocketAcceptorThread();
		} else {
			tcp_serverSocket = null;
			tcp_acceptorThread = null;
		}
		
		if(hasUdp || hasLan) {
			udp_serverSocket = new DatagramSocket(null);
			udp_decoderPool = new AddressBasedDecoderPool(UdpAnonymousConnectionAdapter::new, this);
			udp_receiverThread = new DatagramReceiverThread(udp_serverSocket, this::decideUdpDataDestination,
					this::notifyUdpReceiverThreadClosure, getConfig().getDatagramPacketMaxSize());
			udp_toByteConverter = createToByteConverter();
		} else {
			udp_serverSocket = null;
			udp_decoderPool = null;
			udp_receiverThread = null;
			udp_toByteConverter = null;
		}	
	}

	//STATE GET//
	
	/**
	 * Whether this manager supports UDP/Datagram connections.
	 * This does not apply to UDP server info request.
	 * @return {@code true} if UDP connections are supported, {@code false} otherwise
	 */
	public boolean supportsUdp() {
		return hasUdp;
	}

	
	//SERVER STARTUP IMPLEMENTATION//
	
	private void startTcpImpl() throws IOException {
		tcp_serverSocket.bind(getLocalID().getFeature(NetworkIDFeature.BIND)); //Exception Here -> thread not yet started
		tcp_acceptorThread.start();
	}
	
	private void startUdpLanImpl() throws IOException {
		udp_serverSocket.bind(getLocalID().getFeature(NetworkIDFeature.BIND));
		udp_receiverThread.start();
	}
	

	@Override
	protected Task startServerImpl() {
		if(hasTcp) {
			try {
				startTcpImpl();
			} catch (IOException e) {
				LOGGER.error("Cannot start SocketNetworkManagerServer TCP module", e);
				//Short-circuit return: No other components have been started yet
				currentState = ServerManagerState.STOPPED;
				return Task.completed();
			}
		}

		if(hasUdp || hasLan) {
			try {
				startUdpLanImpl();
			} catch (IOException e) {
				LOGGER.error("Cannot start SocketNetworkManagerServer UDP module", e);
				//Also disable the tcp module that was started before
				if(hasTcp) stopTcpImpl();
				currentState = ServerManagerState.STOPPED;
				return Task.completed();
			}
		}

		LOGGER.info("...Server started");
		currentState = ServerManagerState.RUNNING;
		return Task.completed();
	}

	
	//SERVER SHUTDOWN IMPLEMENTATIONS//
	
	private void stopTcpImpl() {
		//this also closes the socket
		tcp_acceptorThread.interrupt();
	}
	
	private void stopUdpLanImpl() {
		//Will also close the associated socket
		udp_receiverThread.interrupt();
	}
	
	@Override
	protected Task stopServerImpl() {
		if(hasTcp) {
			stopTcpImpl();
		}

		if(hasUdp || hasLan) {
			stopUdpLanImpl();
		}
		currentState = ServerManagerState.STOPPED;
		LOGGER.info("... Server stopped (%s)", getLocalID().getDescription());
		return Task.completed();
	}

	@Internal
	void notifyTcpAcceptorThreadClosure(AcceptorThreadDeathReason reason) {
		if(hasTcp) {
			LOGGER.debug("Ignoring TCP thread death notification %s: No cleanup required, server keeps running for other modules", reason);
		} else {
			LOGGER.warning("SocketNetworkManagerServer was notified of acceptor thread death despite not managing a TCP module");
		}
	}

	@Internal
	void notifyUdpReceiverThreadClosure(AcceptorThreadDeathReason reason) {
		if(hasUdp) {
			LOGGER.debug("Ignoring UDP thread death notification %s: No cleanup required, server keeps running for other modules", reason);
		} else {
			LOGGER.warning("SocketNetworkManagerServer was notified of receiver thread death despite not managing a UDP module");
		}
	}
	
	@Override
	protected void sendDatagram(SocketAddress address, ByteBuffer buffer) throws IOException {
		final byte[] array = new byte[buffer.remaining()];
		buffer.get(array);
		udp_serverSocket.send(new DatagramPacket(array, array.length, address));
	}
	
	private void decideUdpDataDestination(InetSocketAddress address, ByteBuffer buffer) {
		decideUdpDataDestination(address, udp_decoderPool, buffer);
	}
	
	@Override
	protected void sendUdpLogout(SocketAddress remoteAddress) {
		sendRawUdpByteData(remoteAddress, udp_toByteConverter.convert(NetworkPacketFormats.LOGOUT, null));
	}
	
	static final Logger SAT_LOGGER = NetworkManager.getModuleLogger("server-accept");
	private static final AtomicInteger SAT_THREAD_ID = new AtomicInteger(0);
	
	/**
	 * Listens for incoming socket connections for a server.<p>
	 * End the thread by interrupting it ({@link #interrupt()}).
	 */
	private class ServerSocketAcceptorThread extends Thread {

		public ServerSocketAcceptorThread() {
			super("ServerSocketAcceptorThread-" + SAT_THREAD_ID.getAndIncrement());
		}

		@Override
		public void run() {
			AcceptorThreadDeathReason deathReason = AcceptorThreadDeathReason.UNKNOWN;
			try {
				SAT_LOGGER.info("TCP ServerSocket listener thread started");
				while(!this.isInterrupted()) {
					try {
						final Socket socket = tcp_serverSocket.accept();
						acceptIncomingRawTcpConnection(socket, (id, data) ->
							new TcpSocketNetworkConnection(SocketNetworkManagerServer.this, id, socket, data));
					} catch (SocketException e) {
						if(this.isInterrupted()) {
							deathReason = AcceptorThreadDeathReason.INTERRUPTED;
						} else {
							deathReason = AcceptorThreadDeathReason.EXTERNAL;
							SAT_LOGGER.error("TCP ServerSocket listener thread: Unexpected error", e);
						}
						return;
					} catch (SocketTimeoutException e) {
						deathReason = AcceptorThreadDeathReason.TIMEOUT;
						SAT_LOGGER.error("TCP ServerSocket listener thread: Unexpected error", e);
						return;
					} catch (IOException e) {
						deathReason = AcceptorThreadDeathReason.IOEXCEPTION;
						SAT_LOGGER.error("TCP ServerSocket listener thread: Unexpected error", e);
						return;
					}
				}
			} finally {
				//Always run these, however the thread ends
				notifyTcpAcceptorThreadClosure(deathReason);
				SAT_LOGGER.info("TCP ServerSocket listener thread ended with reason %s", deathReason);
			}
		}

		@Override
		public void interrupt() {
			super.interrupt();
			//This will produce a SocketException in the accept() method, ending the loop
			if(isInterrupted() && !tcp_serverSocket.isClosed()) {
				try {
					tcp_serverSocket.close();
				} catch (IOException e) {
					LOGGER.error("Cannot close the server socket for listener thread interrupt");
				}
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
					new UdpServerSocketNetworkConnection(SocketNetworkManagerServer.this, id, data));
			} else {
				LOGGER.debug("Received a UDP-Login for server %s, but UDP module is disabled", getLocalID().getDescription());
			}
		}

		@Override
		public void receiveServerInfoRequest() {
			SocketNetworkManagerServer.this.receiveServerInfoRequest(address);
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
