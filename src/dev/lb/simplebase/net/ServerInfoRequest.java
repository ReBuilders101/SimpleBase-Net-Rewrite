package dev.lb.simplebase.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.connection.DatagramReceiverThread;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.event.EventDispatcher;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFeature;
import dev.lb.simplebase.net.log.Logger;
import dev.lb.simplebase.net.manager.AcceptorThreadDeathReason;
import dev.lb.simplebase.net.manager.NetworkManagerProperties;
import dev.lb.simplebase.net.manager.NetworkManagerServer;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.converter.AddressBasedDecoderPool;
import dev.lb.simplebase.net.packet.converter.AnonymousClientConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.MutableAddressConnectionAdapter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;
import dev.lb.simplebase.net.task.Task;
import dev.lb.simplebase.net.task.ValueTask;
import dev.lb.simplebase.net.util.MonitorBasedThreadsafeIterable;
import dev.lb.simplebase.net.util.Pair;
import dev.lb.simplebase.net.util.ThreadsafeIterable;

/**
 * The {@link ServerInfoRequest} class offers methods to request server info packets from a aserver without making a
 * permanent {@link NetworkConnection} to that server.
 * <p>
 * The queried server must have {@link ServerConfig#getServerInfoPacket()} enabled and configured to return a {@link Packet}.
 * This packet will then be sent to the requester.
 * </p><p>
 * This feature can be used to check server status or to populate a server list with information about the server (e.g. a name or motd)
 * without creating a connection.
 * </p>
 */
public final class ServerInfoRequest {
	private static final Logger LOGGER = NetworkManager.getModuleLogger("server-info");
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
	private final DatagramReceiverThread thread;
	private final AddressBasedDecoderPool pooledDecoders;
	private final Map<InetSocketAddress, CompletableRequestToken> activeRequests;
	private final ThreadsafeIterable<ServerInfoRequest, InetSocketAddress> threadsafe;
	
	private ServerInfoRequest(NetworkManagerProperties manager) throws IOException {
		if(broadcastAddress == null) throw new IllegalStateException("Broadcast address not initialized");
		
		final CommonConfig config = manager.getConfig();
		this.channel = DatagramChannel.open();
		this.activeRequests = new HashMap<>();
		this.threadsafe = new MonitorBasedThreadsafeIterable<>(activeRequests, this, () -> activeRequests.keySet());
		this.encoder = new PacketToByteConverter(manager);
		this.pooledDecoders = new AddressBasedDecoderPool(Adapter::new, manager);
		this.thread = new DatagramReceiverThread(null, channel, pooledDecoders::decode, this::notifyAcceptorThreadDeath, config.getDatagramPacketMaxSize());
		//Just to be sure. Use it like a socket that accepts byte buffers
		this.channel.configureBlocking(true);
		this.channel.socket().setBroadcast(true);
		//This one shouldn't prevent shutdown, since it is not an established connection or a running server
		this.thread.setDaemon(true);
		this.thread.start();
	}
	
	/**
	 * Starts a request for a server info packet to the given {@link NetworkID}.
	 * <ul>
	 * <li>If the {@code NetworkID} implements {@link NetworkIDFeature#INTERNAL}, the internal server is looked up.<br>
	 * If no server is found, the returned {@link ValueTask} will be cancelled with an {@link IllegalArgumentException}.<br>
	 * If the server is found, but does not respond with a packet, the returned {@code ValueTask} will be
	 * cancelled with an {@link UnsupportedOperationException}.<br>
	 * Otherwise, thie returned task will be completed immediately with the response packet.</li>
	 * <li>If the {@code NetworkID} implements {@link NetworkIDFeature#CONNECT} instead, a server info request will be sent
	 * to the server. The returned {@code ValueTask} will be lazily populated with the response packet once it arrives, or it will
	 * be cancelled with an {@link IOException} when {@link #removeActiveRequest(InetSocketAddress)} is called for that address.</li>
	 * <li>If neither function is implemented, the {@code NetworkID} is invalid and this method throws an {@link IllegalArgumentException}</li>
	 * </ul>
	 * @param remote The {@link NetworkID} of the server that should be queried for an info packet
	 * @return A {@link ValueTask} that holds or will hold the response packet once it arrives
	 * @throws IllegalArgumentException When the {@code NetworkID} is invalid
	 * @throws NullPointerException When the {@code NetworkID} is {@code null}
	 */
	public ValueTask<Packet> startRequest(NetworkID remote) {
		Objects.requireNonNull(remote, "'remote' parameter must not be null");
		if(remote.hasFeature(NetworkIDFeature.CONNECT)) {
			return startRequest(remote.getFeature(NetworkIDFeature.CONNECT));
		} else if(remote.hasFeature(NetworkIDFeature.INTERNAL)) {
			final NetworkManagerServer server = InternalServerProvider.getServer(remote);
			if(server == null) {
				return ValueTask.cancelled(new IllegalArgumentException(
						"The requested internal server was not found"), Packet.class);
			}
			final Packet infoPacket = server.createServerInfoPacket();
			if(infoPacket == null) {
				return ValueTask.cancelled(new UnsupportedOperationException(
						"The requested internal server does not supply an info packet"), Packet.class);
			}
			return ValueTask.success(infoPacket);
		} else {
			throw new IllegalArgumentException("NetworkID must implement CONNECT or INTERNAL function");
		}
	}
	
	/**
	 * Sends a server info request to the IPv4 LAN broadcast address (255.255.255.255).
	 * <p>
	 * This method can be used to discover any servers running in the same local network as the requesting client.
	 * Unlike {@link #startRequest(InetSocketAddress)}, this request can be answered with more than one packet.
	 * </p><p>
	 * If sending the request fails with an {@link IOException}, the returned {@code MultiRequestTask} will be cancelled with that exception.
	 * </p><p>
	 * There can only ever be one active LAN request. Use {@link #removeActiveLanRequest()} before starting a new request.<br>
	 * If a LAN request is still active, the returned task will be cancelled with an {@link IllegalStateException}.
	 * </p>
	 * @param port The port on which the serves are running
	 * @return A {@link MultiRequestTask} that handles the response packet
	 */
	public MultiRequestTask startLanRequest(int port) {
		final ByteBuffer data = encoder.convert(NetworkPacketFormats.SERVERINFORQ, null);
		final InetSocketAddress address = new InetSocketAddress(broadcastAddress, port);
		try {
			synchronized (activeRequests) {
				if(activeRequests.containsKey(address)) {
					LOGGER.warning("A request for the LAN address is still pending (%s)", address);
					return MultiRequestTask.cancelled(new IllegalStateException("A LAN request is still active"));
				} else {
					channel.send(data, address);
					final LANRequestToken token = new LANRequestToken(address);
					activeRequests.put(address, token);
					return token;
				}
			}
		} catch (IOException e) {
			LOGGER.error("Cannot send server info request", e);
			return MultiRequestTask.cancelled(e);
		}
	}
	
	/**
	 * Starts a request for a server info packet to the given {@link InetSocketAddress}.
	 * <p>
	 * A server info request will be sent to the server at that address. The returned {@code ValueTask}
	 * will be lazily populated with the response packet once it arrives, or it will
	 * be cancelled with an {@link IOException} when {@link #removeActiveRequest(InetSocketAddress)}
	 * is called for that address.
	 * </p><p>
	 * If sending the request fails with an {@link IOException}, the returned {@code ValueTask} will be cancelled with that exception.
	 * </p><p>
	 * There can only ever be one active request per {@link InetSocketAddress}. Use
	 * {@link #removeActiveRequest(InetSocketAddress)} before starting a new request.<br>
	 * If a request is still active, the returned task will be cancelled with an {@link IllegalStateException}.
	 * </p>
	 * @param remote The {@link NetworkID} of the server that should be queried for an info packet
	 * @return A {@link ValueTask} that holds or will hold the response packet once it arrives
	 * @throws NullPointerException When the {@code InetSocketAddress} is {@code null}
	 */
	public ValueTask<Packet> startRequest(InetSocketAddress remote) {
		Objects.requireNonNull(remote, "'remote' parameter must not be null");
		try {
			synchronized (activeRequests) {
				if(activeRequests.containsKey(remote)) {
					LOGGER.warning("A request for the address is still pending (%s)", remote);
					return ValueTask.cancelled(new IllegalStateException("A request for the address " + 
							remote + " is still active"), Packet.class);
				} else {
					final ByteBuffer data = encoder.convert(NetworkPacketFormats.SERVERINFORQ, null);
					channel.send(data, remote);
					final Pair<ValueTask<Packet>, ValueTask.CompletionSource<Packet>> pair = ValueTask.completable();
					final SingleRequestToken token = new SingleRequestToken(remote, pair.getRight());
					activeRequests.put(remote, token);
					return pair.getLeft();
				}
			}
		} catch (IOException e) {
			LOGGER.error("Cannot send server info request", e);
			return ValueTask.cancelled(e, Packet.class);
		}
	}
	
	/**
	 * Removes a server info request from the list of active requests.
	 * <p>
	 * The {@link ValueTask} associated with that request will be cancelled with an {@link IOException}
	 * and removed from the list of active requests. If a packet arrives from that remote after the request has
	 * been removed, the received packet will <b>not</b> be sent to the {@code ValueTask} and will be discarded.
	 * </p><p>
	 * If no active task is associated with this {@link InetSocketAddress}, a warning is logged.
	 * </p>
	 * @param remote The {@link InetSocketAddress} of the remote server
	 */
	public void removeActiveRequest(InetSocketAddress remote) {
		synchronized (activeRequests) {
			if(activeRequests.containsKey(remote)) {
				final CompletableRequestToken token = activeRequests.get(remote);
				token.cancelRequest(new IOException("Request cancelled by user"));
				activeRequests.remove(remote);
			} else {
				LOGGER.warning("Cannot remove an inactive request token");
			}
		}
	}
	
	/**
	 * Removes the current server info LAN request from the list of active requests.
	 * <p>
	 * The {@link MultiRequestTask} associated with that request will be cancelled with an {@link IOException}
	 * and removed from the list of active requests. If a packet arrives from that remote after the request has
	 * been removed, the received packet will <b>not</b> be sent to the {@code MultiRequestTask} and will be discarded.
	 * </p><p>
	 * If no LAN request is active, a warning is logged.
	 * </p>
	 */
	public void removeActiveLanRequest() {
		synchronized (activeRequests) {
			final CompletableRequestToken lan = getLanRequest();
			if(lan != null) {
				lan.cancelRequest(new IOException("LAN request cancelled by user"));
				activeRequests.remove(lan.address);
			} else {
				LOGGER.warning("Cannot remove LAN request token");
			}
		}
	}
	
	/**
	 * Checks whether a server info request is active for that address
	 * <p>
	 * A server info request is considered active if it has been sent, but an answer is still pending.
	 * The request will be removed from the active request list when the response arrives, or when it is
	 * removed by calling {@link #removeActiveRequest(InetSocketAddress)}.
	 * </p>
	 * @param address The address to which the request was sent
	 * @return Whether a request was sent to that address and an answer is still pending
	 */
	public boolean isRequestActive(InetSocketAddress address) {
		synchronized (activeRequests) {
			return activeRequests.containsKey(address);
		}
	}
	
	/**
	 * Checks whether a server info LAN request is active
	 * <p>
	 * The amount of expected answers to a LAN broadcast request is unknown by nature, so a LAN request will never
	 * be removed from the active request list on its own. It can be removed by calling {@link #removeActiveLanRequest()}.
	 * </p>
	 * @return Whether a LAN request was sent and not yet removed
	 */
	public boolean isLanRequestActive() {
		return getLanRequest() != null;
	}
	
	/**
	 * The amount of all requests that have been sent out, but not answered yet.<br>
	 * Requests removed with {@link #removeActiveRequest(InetSocketAddress)} are not included in the count.
	 * @return The amount of currently active requset
	 */
	public int getActiveRequestCount() {
		synchronized (activeRequests) {
			return activeRequests.size();
		}
	}
	
	/**
	 * Provides threadsafe access to this {@link ServerInfoRequest} object and its active task list.
	 * While locked on to the task list, no requests will be added or have their state altered.
	 * @return An object that safely accesses the task list
	 */
	public ThreadsafeIterable<ServerInfoRequest, InetSocketAddress> threadsafe() {
		return threadsafe;
	}
	
	private CompletableRequestToken getLanRequest() {
		synchronized (activeRequests) {
			//Loop, b/c we don't have the exact address, only the ip
			for(Entry<InetSocketAddress, CompletableRequestToken> a : activeRequests.entrySet()) {
				//InetAddress doc says a.getAddress() can be null, so also check the literal name
				if(broadcastAddress.equals(a.getKey().getAddress()) || "255.255.255.255".equals(a.getKey().getHostString())) {
					return a.getValue();
				}
			}
			return null;
		}
	}
	
	private void receiveServerInfoPacket(InetSocketAddress source, Packet packet) {
		synchronized (activeRequests) {
			final CompletableRequestToken token = activeRequests.get(source);
			if(token == null) {
				//Probably a broadcast
				final CompletableRequestToken crt = getLanRequest();
				if(crt != null) {
					if(crt.supplyAnswer(packet)) activeRequests.remove(source);
				} else {
					LOGGER.warning("Received packet for an address with no active token (%s)", source);
				}
			} else {
				if(token.supplyAnswer(packet)) activeRequests.remove(source);
			}
		}
	}
	
	private void notifyAcceptorThreadDeath(AcceptorThreadDeathReason reason) {
		LOGGER.debug("Ignoring Server Info Request thread death notification %s: Likely cleaning up right now", reason);
	}
	
	private static abstract class CompletableRequestToken {
		protected final InetSocketAddress address;
		
		/**
		 * @param packet The packet that completes the token
		 * @return Whether the token should be removed from the list
		 */
		protected abstract boolean supplyAnswer(Packet packet);
		
		protected abstract void cancelRequest(Throwable error);
		
		protected CompletableRequestToken(InetSocketAddress address) {
			this.address = address;
		}
	}
	
	private static final class SingleRequestToken extends CompletableRequestToken {
		private final ValueTask.CompletionSource<Packet> completionSource;
		
		protected SingleRequestToken(InetSocketAddress address, ValueTask.CompletionSource<Packet> completionSource) {
			super(address);
			this.completionSource = completionSource;
		}

		@Override
		protected boolean supplyAnswer(Packet packet) {
			completionSource.success(packet);
			return true;
		}

		@Override
		protected void cancelRequest(Throwable error) {
			completionSource.cancelled(error);
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
	
	private static final class LANRequestToken extends CompletableRequestToken implements MultiRequestTask {
		
		private final List<Consumer<Packet>> onPacket;
		private final List<Consumer<ExecutionException>> onCancelled;
		private final List<Packet> packets;
		
		private final long startTime;
		private volatile Pair<ValueTask<Packet>, ValueTask.CompletionSource<Packet>> nextAction;
		
		protected LANRequestToken(InetSocketAddress address) {
			super(address);
			this.onPacket = new LinkedList<>();
			this.onCancelled = new LinkedList<>();
			this.packets = new LinkedList<>();
			this.nextAction = ValueTask.completable();
			this.startTime = NetworkManager.getClockMillis();
		}

		@Override
		public synchronized MultiRequestTask onPacketReceived(Consumer<Packet> packet) {
			packets.forEach((p) -> packet.accept(p));
			onPacket.add(packet);
			return this;
		}

		@Override
		public synchronized MultiRequestTask onCancelled(Consumer<ExecutionException> exception) {
			if(isCancelled()) exception.accept(nextAction.getLeft().getCancelled());
			onCancelled.add(exception);
			return this;
		}

		@Override
		public synchronized MultiRequestTask onPacketReceivedAsync(Consumer<Packet> packet) {
			packets.forEach((p) -> CompletableFuture.runAsync(() -> packet.accept(p)));
			onPacket.add((p) -> CompletableFuture.runAsync(() -> packet.accept(p)));
			return this;
		}

		@Override
		public synchronized MultiRequestTask onCancelledAsync(Consumer<ExecutionException> exception) {
			if(isCancelled()) CompletableFuture.runAsync(() -> exception.accept(nextAction.getLeft().getCancelled()));
			onCancelled.add((e) -> CompletableFuture.runAsync(() -> exception.accept(e)));
			return this;
		}

		@Override
		public synchronized ValueTask<Packet> getNextPacketTask() {
			return nextAction.getLeft();
		}

		@Override
		public MultiRequestTask await(long timeout, TimeUnit unit) {
			try {
				tryAwait(timeout, unit);
				return this;
			} catch (InterruptedException | TimeoutException e) {
				return this;
			}
		}

		@Override
		public MultiRequestTask tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
			final long ms = unit.toMillis(timeout);
			Thread.sleep(ms);
			throw new TimeoutException();
		}

		@Override
		public boolean asyncAwait(long timeout, TimeUnit unit) {
			if(isCancelled()) return true;
			long timeoutMs = unit.toMillis(timeout);
			return NetworkManager.getClockMillis() > startTime + timeoutMs;
		}

		@Override
		protected synchronized boolean supplyAnswer(Packet packet) {
			onPacket.forEach((c) -> c.accept(packet));
			packets.add(packet);
			nextAction.getRight().success(packet);
			nextAction = ValueTask.completable();
			return false;
		}

		@Override
		protected synchronized void cancelRequest(Throwable error) {
			final ExecutionException commonEx = new ExecutionException(error);
			onCancelled.forEach((e) -> e.accept(commonEx));
			nextAction.getRight().cancelled(error);
			//Don't update nextAction, it will remain cancelled
		}

		@Override
		public synchronized boolean isCancelled() {
			return nextAction.getLeft().isCancelled();
		}

		@Override
		public Stream<Packet> getReceivedPackets() {
			return packets.stream();
		}

		@Override
		public ExecutionException getCancelled() throws IllegalStateException {
			if(isCancelled()) {
				return nextAction.getLeft().getCancelled();
			} else {
				throw new IllegalStateException("MultiRequestTask is not cancelled");
			}
		}
	}
	
	/**
	 * Represents a future response to a LAN server info request.
	 * <p>
	 * Will behave similar to a {@link ValueTask}{@code<Packet>}, except that it can receive
	 * a packet more than onec and will never be completed. Cancellation with an exception is still possible.
	 * <br>
	 * Methods to await and to register handlers are provided.
	 * </p>
	 */
	public static interface MultiRequestTask {
		/**
		 * Registers a handler that will run synchroneously when a packet is received.
		 * <p>The handler will run on the thread that sends the received packet to the {@link MultiRequestTask}.</p>
		 * <p>If any packets were received before this handler is added, the handler will run immediately
		 * on the thread calling this method.</p>
		 * @param packet The packet handler that will run for every packet received.
		 * @return {@code this}
		 */
		public MultiRequestTask onPacketReceived(Consumer<Packet> packet);
		/**
		 * Registers a handler that will run synchroneously when a this task is cancelled
		 * <p>The handler will run on the thread that cancelled {@link MultiRequestTask}.</p>
		 * <p>If the task was already cancelled when this method is called, the handler will
		 * run immediately on the thread that called this method.</p>
		 * @param exception The handler that will run when the task is cancelled
		 * @return {@code this}
		 */
		public MultiRequestTask onCancelled(Consumer<ExecutionException> exception);
		/**
		 * Registers a handler that will run asynchroneously when a packet is received.
		 * <p>The handler will run a thread pool, by default using the {@link ForkJoinPool#commonPool()}.</p>
		 * <p>If any packets were received before this handler is added, the handler will run immediately
		 * for those packets.</p>
		 * @param packet The packet handler that will run for every packet received.
		 * @return {@code this}
		 */
		public MultiRequestTask onPacketReceivedAsync(Consumer<Packet> packet);
		/**
		 * Registers a handler that will run asynchroneously when a this task is cancelled
		 * <p>The handler will run a thread pool, by default using the {@link ForkJoinPool#commonPool()}.</p>
		 * <p>If the task was already cancelled when this method is called, the handler will
		 * run immediately.</p>
		 * @param exception The handler that will run when the task is cancelled
		 * @return {@code this}
		 */
		public MultiRequestTask onCancelledAsync(Consumer<ExecutionException> exception);
		
		/**
		 * Creates a regular {@link ValueTask} that completes when the next packet is received.
		 * <p>
		 * When this {@link MultiRequestTask} is cancelled, the returned {@code ValueTask} will
		 * be initally cancelled with the same description.
		 * </p>
		 * @return A task that waits for the next packet
		 */
		public ValueTask<Packet> getNextPacketTask();
		
		/**
		 * Blocks the calling thread until this task is cancelled, until this thread is interrupted, or until the timeout expires.
		 * Will return immediately if this {@code MultiRequestTask} is already cancelled.
		 * <p>
		 * Unlike a regular {@link Task} or {@link ValueTask}, a {@link MultiRequestTask} cannot be completed as there might still be
		 * packets coming form other servers.
		 * </p>
		 * @param timeout The amount of time to wait
		 * @param unit The {@link TimeUnit} for the timeout
		 * @return {@code this}
		 * @see #asyncAwait(long, TimeUnit)
		 */
		public MultiRequestTask await(long timeout, TimeUnit unit);
		/**
		 * Blocks the calling thread until this task is cancelled, until this thread is interrupted, or until the timeout expires.
		 * Will return immediately if this {@code MultiRequestTask} is already cancelled.
		 * <p>
		 * Unlike a regular {@link Task} or {@link ValueTask}, a {@link MultiRequestTask} cannot be completed as there might still be
		 * packets coming form other servers.
		 * </p>
		 * @param timeout The amount of time to wait
		 * @param unit The {@link TimeUnit} for the timeout
		 * @return {@code this}
		 * @throws InterruptedException If the current thread is interrupted while waiting
		 * @throws TimeoutException If the timeout expires before the task was cancelled
		 * @see #await(long, TimeUnit)
		 */
		public MultiRequestTask tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;
		
		/**
		 * Non-blocking await method. This method will never block the calling thread.
		 * <p>
		 * Can be used to detect whether a certain amount of time (the timeout) has passed since this task has been started.<br>
		 * The timeout is relative to the start time of the task, not to the first invocation of the {@code asyncAwait()} method.
		 * </p>
		 * @param timeout The amount of time to wait
		 * @param unit The {@link TimeUnit} for the timeout
		 * @return  {@code True} if the timeout has passed since creation of the {@link MultiRequestTask}, or if the task is cancelled.<br>
		 * 			{@code False} if the task is still running and the timeout has not expired.
		 */
		public boolean asyncAwait(long timeout, TimeUnit unit);
		
		/**
		 * If {@code true}, the task has been cancelled.
		 * <p>
		 * A cancelled {@link MultiRequestTask} can no longer receive packets and is removed from the {@link ServerInfoRequest}s
		 * active task list. The exception that caused cancellation can be retrieved with {@link #getCancelled()}.<br>
		 * Awaiting a cancelled task will never block. 
		 * </p>
		 * @return Whether this task is cancelled
		 */
		public boolean isCancelled();
		
		/**
		 * The exception that caused this task to be cancelled.
		 * <p>
		 * The exception is only present when the task has been cancelled.
		 * </p>
		 * @return The exception that caused cancellation
		 * @throws IllegalStateException When the task has not been cancelled
		 * @see #isCancelled()
		 */
		public ExecutionException getCancelled() throws IllegalStateException;
		
		/**
		 * A stream of all packets that have been received as responses to the server info request so far.
		 * <p>
		 * As any {@link Stream}, this one is lazily evaluated once a terminal method has been called for the stream.
		 * </p>
		 * @return A {@link Stream} of all received packets
		 */
		public Stream<Packet> getReceivedPackets();
		
		/**
		 * Creates a {@link MultiRequestTask} that is pre-cancelled with the supplied {@link Throwable}.
		 * @param throwable The throwable that caused cancellation
		 * @return The cancelled {@link MultiRequestTask}
		 */
		public static MultiRequestTask cancelled(Throwable throwable) {
			final ExecutionException ex = new ExecutionException(throwable);
			final ValueTask<Packet> task = ValueTask.cancelled(throwable, Packet.class);
			return new MultiRequestTask() {
				
				@Override
				public MultiRequestTask tryAwait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
					return this;
				}
				
				@Override
				public MultiRequestTask onPacketReceivedAsync(Consumer<Packet> packet) {
					return this;
				}
				
				@Override
				public MultiRequestTask onPacketReceived(Consumer<Packet> packet) {
					return this;
				}
				
				@Override
				public MultiRequestTask onCancelledAsync(Consumer<ExecutionException> exception) {
					CompletableFuture.runAsync(() -> exception.accept(ex));
					return this;
				}
				
				@Override
				public MultiRequestTask onCancelled(Consumer<ExecutionException> exception) {
					exception.accept(ex);
					return this;
				}
				
				@Override
				public boolean isCancelled() {
					return true;
				}
				
				@Override
				public ValueTask<Packet> getNextPacketTask() {
					return task;
				}
				
				@Override
				public MultiRequestTask await(long timeout, TimeUnit unit) {
					return this;
				}
				
				@Override
				public boolean asyncAwait(long timeout, TimeUnit unit) {
					return true;
				}

				@Override
				public Stream<Packet> getReceivedPackets() {
					return Stream.empty();
				}

				@Override
				public ExecutionException getCancelled() throws IllegalStateException {
					return ex;
				}
			};
		}
	}
	
	/**
	 * Creates a new {@link ServerInfoRequest} instance that can be used to start server info packet requests.
	 * @param mappings The {@link PacketIDMappingProvider} that contains at least hte mapping(s) for the received info packet(s)
	 * @param config A {@link CommonConfig} that sets options used for the encde/decode buffers and thread pools
	 * @return A new {@link ServerInfoRequest} with those settings
	 */
	public static ServerInfoRequest create(PacketIDMappingProvider mappings, CommonConfig config) {
		return create(NetworkManagerProperties.of(config, mappings, 
				EventDispatcher.emptyDispatcher().p1Dispatcher(null, null),
				EventDispatcher.emptyDispatcher().p1Dispatcher(null, null)		
		));
	}
	
	/**
	 * Creates a new {@link ServerInfoRequest} instance that can be used to start server info packet requests.
	 * @param template A template {@link NetworkManagerProperties} object. The {@code ServerInfoRequest} will use the templates
	 * {@code PacketIDMappingProvider} and {@code CommonConfig}.<br>
	 * If enabled in the config, it will also <b>use the template's thread pools</b>.
	 * @return A new {@link ServerInfoRequest} with those settings
	 */
	public static ServerInfoRequest create(NetworkManagerProperties template) {
		try {
			return new ServerInfoRequest(template);
		} catch (IOException e) {
			LOGGER.error("Cannot create channel", e);
			return null;
		}
	}
}
