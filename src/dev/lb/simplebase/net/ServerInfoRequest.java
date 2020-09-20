package dev.lb.simplebase.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.connection.DatagramSocketReceiverThread;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.manager.AcceptorThreadDeathReason;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.manager.NetworkManagerProperties;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.converter.AddressBasedDecoderPool;
import dev.lb.simplebase.net.packet.converter.AnonymousClientConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.ByteToPacketConverter;
import dev.lb.simplebase.net.packet.converter.MutableAddressConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;
import dev.lb.simplebase.net.util.AwaitableTask;
import dev.lb.simplebase.net.util.Task;

public final class ServerInfoRequest {
	private static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("server-info");
	private static final InetAddress broadcastAddress;
	
	static {
		InetAddress address;
		try {
			address = InetAddress.getByName("255.255.255.255");
		} catch (UnknownHostException e) {
			LOGGER.error("Cannot resolve the LAN broadcast address", e);
			address = null;
		}
		broadcastAddress = address;
	}
	
	private final DatagramChannel channel;
	private final PacketToByteConverter encoder;
	private final DatagramSocketReceiverThread thread;
	private final AddressBasedDecoderPool pooledDecoders;
	private final Map<InetSocketAddress, CompletableToken> activeRequests;
	
	private ServerInfoRequest(NetworkManagerProperties manager) throws IOException {
		if(broadcastAddress == null) throw new IllegalStateException("Broadcast address not initialized");
		
		final CommonConfig<?> config = manager.getConfig();
		this.channel = DatagramChannel.open();
		this.activeRequests = new HashMap<>();
		this.encoder = new PacketToByteConverter(manager.getMappingContainer(), config.getPacketBufferInitialSize(), config.getCompressionSize());
		this.pooledDecoders = new AddressBasedDecoderPool(Adapter::new, manager);
		this.thread = new DatagramSocketReceiverThread(channel.socket(), pooledDecoders::decode, this::notifyAcceptorThreadDeath, config.getDatagramPacketMaxSize());
		//Just to be sure. Use it like a socket that accepts byte buffers
		this.channel.configureBlocking(true);
		this.channel.socket().setBroadcast(true);
		//This one shouldn't prevent shutdown, since it is not an established connection or a running server
		this.thread.setDaemon(true);
		this.thread.start();
	}
	
	public RequestToken startRequest(InetSocketAddress remote) {
		return startRequest(remote, null);
	}
	
	public MultiRequestToken startLanRequest(int port) {
		return startLanRequest(port, null);
	}
	
	public RequestToken startRequest(NetworkID remote) {
		return startRequest(remote, null);
	}
	
	public RequestToken startRequest(NetworkID remote, Consumer<Packet> callback) {
		if(remote.hasFunction(NetworkIDFunction.CONNECT)) {
			return startRequest(remote.getFunction(NetworkIDFunction.CONNECT), callback);
		} else {
			throw new IllegalArgumentException("NetworkID must implement CONNECT function");
		}
	}
	
	public MultiRequestToken startLanRequest(int port, Consumer<Packet> callback) {
		final ByteBuffer data = encoder.convert(NetworkPacketFormats.SERVERINFORQ, null);
		final InetSocketAddress address = new InetSocketAddress(broadcastAddress, port);
		try {
			synchronized (activeRequests) {
				if(activeRequests.containsKey(address)) {
					LOGGER.warning("A request for the LAN address is still pending (%s)", address);
					return null;
				} else {
					channel.send(data, address);
					final MultiRequestToken token = new MultiRequestToken(address, callback);
					activeRequests.put(address, token);
					return token;
				}
			}
		} catch (IOException e) {
			LOGGER.error("Cannot send server info request", e);
			return null;
		}
	}
	
	public RequestToken startRequest(InetSocketAddress remote, Consumer<Packet> callback) {
		final ByteBuffer data = encoder.convert(NetworkPacketFormats.SERVERINFORQ, null);
		try {
			synchronized (activeRequests) {
				if(activeRequests.containsKey(remote)) {
					LOGGER.warning("A request for the address is still pending (%s)", remote);
					return null;
				} else {
					channel.send(data, remote);
					final RequestToken token = new RequestToken(remote, callback);
					activeRequests.put(remote, token);
					return token;
				}
			}
		} catch (IOException e) {
			LOGGER.error("Cannot send server info request", e);
			return null;
		}
	}
	
	public void removeActiveRequest(CompletableToken token) {
		synchronized (activeRequests) {
			if(isRequestActive(token)) {
				token.completeRequest(null);
				activeRequests.remove(token.address);
			} else {
				LOGGER.warning("Cannot remove an inactive request token");
			}
		}
	}
	
	public void removeActiveLanRequest() {
		synchronized (activeRequests) {
			CompletableToken lan = getLanRequest();
			if(lan != null) {
				lan.completeRequest(null);
				activeRequests.remove(lan.address);
			} else {
				LOGGER.warning("Cannot remove LAN request token");
			}
		}
	}
	
	public boolean isRequestActive(CompletableToken token) {
		synchronized (activeRequests) {
			return activeRequests.containsKey(token.address) && activeRequests.get(token.address) == token;
		}
	}
	
	public boolean isRequestActive(InetSocketAddress address) {
		synchronized (activeRequests) {
			return activeRequests.containsKey(address);
		}
	}
	
	public boolean isLanRequestActive() {
		return getLanRequest() != null;
	}
	
	private CompletableToken getLanRequest() {
		synchronized (activeRequests) {
			//Loop, b/c we don't have the exact address, only the ip
			for(Entry<InetSocketAddress, CompletableToken> a : activeRequests.entrySet()) {
				//InetAddress doc says a.getAddress() can be null, so also check the literal name
				if(broadcastAddress.equals(a.getKey().getAddress()) || "255.255.255.255".equals(a.getKey().getHostString())) {
					return a.getValue();
				}
			}
			return null;
		}
	}
	
	public int getActiveRequestCount() {
		synchronized (activeRequests) {
			return activeRequests.size();
		}
	}
	
	private void receiveServerInfoPacket(InetSocketAddress source, Packet packet) {
		synchronized (activeRequests) {
			final CompletableToken token = activeRequests.get(source);
			if(token == null) {
				LOGGER.warning("Received packet for an address with no active token (%s)", source);
			} else {
				if(token.completeRequest(packet)) activeRequests.remove(source);
			}
		}
	}
	
	private void notifyAcceptorThreadDeath(AcceptorThreadDeathReason reason) {
		LOGGER.debug("Ignoring Server Info Request thread death notification %s: Likely cleaning up right now", reason);
	}
	
	private static abstract class CompletableToken {
		protected final InetSocketAddress address;
		
		/**
		 * @return Should the token be removed?
		 */
		protected abstract boolean completeRequest(Packet packet);
		
		public abstract boolean wasCanecelled();
		
		protected CompletableToken(InetSocketAddress address) {
			this.address = address;
		}
	}
	
 	private class Adapter implements MutableAddressConnectionAdapter, AnonymousClientConnectionAdapter {

		private final ReferenceCounter counter;
		private volatile InetSocketAddress address;
		
		public Adapter(InetSocketAddress address) {
			this.counter = new ReferenceCounter();
			this.address = address;
		}

		@Override
		public ReferenceCounter getUseCountManager() {
			return counter;
		}

		@Override
		public void receiveServerInfoPacket(Packet packet) {
			ServerInfoRequest.this.receiveServerInfoPacket(address, packet);
		}

		@Override
		public void setAddress(InetSocketAddress address) {
			this.address = address;
		}
		
	}
	
	public static class RequestToken extends CompletableToken {
		
		private final AwaitableTask task;
		private volatile Packet currentResult;
		
		private RequestToken(InetSocketAddress address, Consumer<Packet> callback) {
			super(address);
			this.task = new AwaitableTask();
			this.currentResult = null;
			if(callback != null) task.then(() -> callback.accept(currentResult));
		}
		
		public Task getTask() {
			return task;
		}
		
		public InetSocketAddress getRequestAddress() {
			return address;
		}
		
		public Packet getResult() throws NoSuchElementException {
			if(!task.isDone() || currentResult == null) {
				throw new NoSuchElementException("Packet has not been received yet");
			} else {
				return currentResult;
			}
		}
		
		@Override
		protected boolean completeRequest(Packet packet) {
			this.currentResult = packet;
			this.task.release();
			return true;
		}
		
		@Override
		public boolean wasCanecelled() {
			return task.isDone() && currentResult == null;
		}
	}
	
	public static class MultiRequestToken extends CompletableToken {
		
		private final Consumer<Packet> callback;
		private final Set<Packet> packets;
		private volatile boolean cancelled;
		
		protected MultiRequestToken(InetSocketAddress address, Consumer<Packet> callback) {
			super(address);
			this.callback = callback;
			this.packets = Collections.synchronizedSet(new HashSet<>());
		}
		
		public Set<Packet> getCurrentResults() {
			return Collections.unmodifiableSet(packets);
		}

		@Override
		protected boolean completeRequest(Packet packet) {
			if(cancelled || packet == null) {
				cancelled = true;
			} else {
				packets.add(packet);
				if(callback != null) callback.accept(packet);
			}
			return false;
		}

		@Override
		public boolean wasCanecelled() {
			return cancelled;
		}
	}
	
	public static ServerInfoRequest create(PacketIDMappingProvider mappings, CommonConfig<?> config) {
		try {
			final ByteToPacketConverter singleCon = new ByteToPacketConverter(mappings, config.getCompressionSize());
			ServerInfoRequest req = new ServerInfoRequest(NetworkManagerProperties.of(config, mappings, null, 
					() -> singleCon));
			return req;
		} catch (IOException e) {
			LOGGER.error("Cannot create channel", e);
			return null;
		}
	}
	
	public static ServerInfoRequest create(NetworkManagerCommon template) {
		return create(template.getMappingContainer(), template.getConfig());
	}
}
