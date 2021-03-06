package dev.lb.simplebase.net.manager;

import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import dev.lb.simplebase.net.GlobalTimer;
import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.connection.CoderThreadPool;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.event.EventAccessor;
import dev.lb.simplebase.net.event.EventDispatcher;
import dev.lb.simplebase.net.events.ConnectionClosedEvent;
import dev.lb.simplebase.net.events.PacketSendingFailedEvent;
import dev.lb.simplebase.net.events.PacketReceiveRejectedEvent;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.packet.PacketIDMapping;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.converter.ByteToPacketConverter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;
import dev.lb.simplebase.net.packet.handler.EmptyPacketHandler;
import dev.lb.simplebase.net.packet.handler.PacketHandler;
import dev.lb.simplebase.net.packet.handler.ThreadPacketHandler;
import dev.lb.simplebase.net.util.InternalAccess;
import dev.lb.simplebase.net.util.Lazy;

/**
 * The base class for both server and client managers.
 */
@Threadsafe
public abstract class NetworkManagerCommon implements NetworkManagerProperties {
	
	/**
	 * The {@link ConnectionClosedEvent} will be posted to this accessor when a connection handled by this manager is
	 * closed.
	 */
	public final EventAccessor<ConnectionClosedEvent> ConnectionClosed;
	
	/**
	 * The {@link PacketSendingFailedEvent} will be posted to this accessor when a packet could not
	 * be sent through a connection.
	 */
	public final EventAccessor<PacketSendingFailedEvent> PacketSendingFailed;
	
	/**
	 * The {@link PacketReceiveRejectedEvent} will be posted to this accessor when a packet couldn't be
	 * received because the queue for the receiver thread was full.<br>
	 * <b>Event handlers for this event should be very fast to avoid building a packet backlog!</b>
	 * Packet receiving for the network connection that received the rejected
	 * packet will be blocked while this event's handlers are running.
	 * <p>
	 * Receiving this event can mean:
	 * <ul>
	 * <li>That the receivers queue size has been set manually to a low value</li>
	 * <li>That there has been a spike in incoming packets that could not be enqueued</li>
	 * <li>That the {@link PacketHandler}s for this manager take to long or block their thread,
	 * so that the queue of unhandled packets overflowed</li>
	 * </ul>
	 * </p>
	 */
	public final EventAccessor<PacketReceiveRejectedEvent> PacketReceiveRejected;
	
	
	
	private final NetworkID local;
	private final CommonConfig config;
	private final PacketIDMappingProvider provider;
	private final AtomicReference<PacketHandler> singleThreadHandler;
	private final ThreadPacketHandler multiThreadHandler;
	private final EventDispatcher dispatcher;
	private final Optional<Thread> managedThread;
	private final CoderThreadPool.Encoder encoderPool;
	private final CoderThreadPool.Decoder decoderPool;
	private final Lazy<PacketToByteConverter> commonToByteConverter;
	private final Lazy<ByteToPacketConverter> commonToPacketConverter;
	
	/**
	 * Constructor that initializes {@link NetworkManagerCommon} base features
	 * @param local The local {@link NetworkID} representing this manager
	 * @param config The configuration object to create this manager. Will be locked if it is not already.
	 */
	protected NetworkManagerCommon(NetworkID local, CommonConfig config, int depth) {
		InternalAccess.assertCaller(NetworkManager.class, depth, "Cannot instantiate NetworkManagerCommon subclasses directly");
		
		this.local = local;
		this.config = config; //It is now locked and can't be changed, so it can be stored
		this.provider = new PacketIDMappingProvider();
		
		//EVENTS
		ConnectionClosed = new EventAccessor<>(ConnectionClosedEvent.class);
		PacketSendingFailed = new EventAccessor<>(PacketSendingFailedEvent.class);
		PacketReceiveRejected = new EventAccessor<>(PacketReceiveRejectedEvent.class);
		
		dispatcher = new EventDispatcher(() -> getLocalID().getDescription());
		encoderPool = stackHackE(config, dispatcher.p1Dispatcher(PacketSendingFailed, PacketSendingFailedEvent::new));
		decoderPool = stackHackD(config, dispatcher.p1Dispatcher(PacketReceiveRejected, PacketReceiveRejectedEvent::new));
		singleThreadHandler = new AtomicReference<>(new EmptyPacketHandler());
		if(config.getUseHandlerThread()) {
			multiThreadHandler = new ThreadPacketHandler(singleThreadHandler, dispatcher.p2Dispatcher(PacketReceiveRejected, 
					(packet, context) -> new PacketReceiveRejectedEvent(context.getRemoteID(), packet.getClass())));
			managedThread = Optional.of(multiThreadHandler.getOutputThread());
		} else {
			multiThreadHandler = null;
			managedThread = Optional.empty();
		}
		
		if(config.getGlobalConnectionCheck()) {
			GlobalTimer.subscribeManagerForConnectionStatusCheck(this);
		}
		
		commonToByteConverter = Lazy.of(() -> new PacketToByteConverter(this));
		commonToPacketConverter = Lazy.of(() -> new ByteToPacketConverter(this));
	}

	/**
	 * The container for {@link PacketIDMapping}s that are used to convert the packets sent form this manager to bytes.
	 * @return A {@link PacketIDMappingProvider} that holds the mappings for this manager
	 */
	@Override
	public final PacketIDMappingProvider getMappingContainer() {
		return provider;
	}
	
	
	/**
	 * Creates a new encoder thread pool using a stack frame in the {@link NetworkManagerCommon} class
	 * @param np The {@link CommonConfig} for the thread pool
	 * @param p1 The error handler for the thread pool
	 * @return A new encoder thread pool
	 */
	@Internal
	static CoderThreadPool.Encoder stackHackE(CommonConfig np, Predicate<RejectedExecutionException> p1) {
		return new CoderThreadPool.Encoder(np, p1);
	}

	/**
	 * Creates a new decoder thread pool using a stack frame in the {@link NetworkManagerCommon} class
	 * @param np The {@link CommonConfig} for the thread pool
	 * @param p1 The error handler for the thread pool
	 * @return A new decoder thread pool
	 */
	@Internal
	static CoderThreadPool.Decoder stackHackD(CommonConfig np, Predicate<RejectedExecutionException> p1) {
		return new CoderThreadPool.Decoder(np, p1);
	}
	
	/**
	 * Add a {@link PacketHandler} that receives incoming packets from all connections.<br>
	 * Depending on whether this manager uses a managed thread, the handlers may receive the packets
	 * on only one thread (no thread safety in handlers needed) or on a different thread for every receiver
	 * (thread safety of handlers required). See {@link #getManagedThread()}.
	 * <p>
	 * If more than one handler is added, all handlers receive the same packet in no fixed order.<br>
	 * In some implementations handling might be faster if only a single handler is added.
	 * </p>
	 * @param handler The handler to add
	 */
	public void addPacketHandler(PacketHandler handler) {
		singleThreadHandler.getAndUpdate((old) -> PacketHandler.combineHandlers(old, handler));
	}
	
	/**
	 * The {@link NetworkID} that identifies this network manager.
	 * @return The local NetworkID
	 */
	public final NetworkID getLocalID() {
		return local;
	}
	
	/**
	 * A network manager offers two basic ways of packet processing:
	 * <ul>
	 * <li>{@link Packet}s can be passed to the {@link PacketHandler}s registered with this manager on the
	 * Thread that they were received from the connectio. This means that the packet handler will be called
	 * on a different thread for packets from diffferent sources, and these calls may occur concurrently.
	 * Additional actions to ensure thread safety of the handlers must be taken.</li>
	 * <li>When using a managed thread, the {@link PacketHandler}s will only ever be called on one thread: The
	 * thread returned by this method. Archieving thread safety in the handlers is easy as they are always
	 * run in a single thread. Because packets from all connection must be processed by a single thread,
	 * packets may be rejected under high load when an internal queue fills up</li>
	 * </ul>
	 * <b>Warning: </b>Interrupting the managed thread will stop packet processing!
	 * @return An empty {@link Optional} if no managed thread is used, otherwise the optional contains the described managed thread
	 */
	public Optional<Thread> getManagedThread() {
		return managedThread;
	}
	
	/**
	 * The amount of unprocessed packets. If no managed thread is used, this method always returns 0.
	 * @return The amount of unprocessed packets.
	 */
	public int getPacketQueueSize() {
		return multiThreadHandler == null ? 0 : multiThreadHandler.getQueueCurrentSize();
	}
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * Use {@link NetworkConnection#receivePacket(Packet)}
	 * to simulate a received packet instead.</p><p>
	 * Push a packet received by a connection to this manager.
	 * The manager can accept packets on any thread and will handle it correctly.
	 * </p>
	 * @param packet The recieved packet
	 * @param context The corresponding {@link PacketContext}
	 */
	@Internal
	public void receivePacketOnConnectionThread(Packet packet, PacketContext context) {
		if(managedThread.isPresent()) {
			multiThreadHandler.handlePacket(packet, context);
		} else {
			singleThreadHandler.get().handlePacket(packet, context);
		}
	}
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * Using this can leave connections in a broken state where 
	 * handler threads might not be stopped.
	 * </p><p>
	 * Removes the connection with the proper synchronization, without causing any other
	 * side effects like closing the connection or posting an event to the dispatcher.
	 * </p>
	 * @param connection The connection to remove
	 */
	@Internal
	public	abstract void removeConnectionSilently(NetworkConnection connection);
	
	/**
	 * Send a packet to a destination {@link NetworkID}, if a connection with that id is present on
	 * this manager.
	 * @param remote The {@link NetworkID} of the packet destination
	 * @param packet The {@link Packet} to send
	 * @return {@code false} if no connection for that {@link NetworkID} was found,
	 * otherwise the result of {@link NetworkConnection#sendPacket(Packet)}
	 */
	public abstract boolean sendPacketTo(NetworkID remote, Packet packet);
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * The {@link EventDispatcher} that posts the events for all {@link EventAccessor}s in the manager class
	 * and subclasses.
	 * </p>
	 * @return The {@link EventDispatcher} for this manager
	 */
	@Internal
	public EventDispatcher getEventDispatcher() {
		return dispatcher;
	}
	
	/**
	 * The configuration object used by this network manager. The returned object will
	 * be locked ({@link CommonConfig#isLocked()}) and effectively immutable.
	 * @return The configuation object for this manager
	 */
	@Override
	public CommonConfig getConfig() {
		return config;
	}
	
	/**
	 * An array of all events that this network manager implementation can post.<br>
	 * Useful combined with {@link EventAccessor#addAllHandlers(Class, EventAccessor...)} to
	 * register handlers for all events on this manager.
	 * <p>
	 * The returned array <b>must not be modified</b>.
	 * @return An array of all event accessors declared by this manager type
	 */
	public abstract EventAccessor<?>[] getEvents();
	
	/**
	 * Cleans up all resources and connections held by this manager. Should be called manually before
	 * the manager is left to garbage collection.
	 */
	public void cleanUp() {
		managedThread.ifPresent(Thread::interrupt);
		encoderPool.shutdown();
		decoderPool.shutdown();
		GlobalTimer.unsubscribeManagerForConnectionStatusCheck(this);
	}
	
	/**
	 * Updates the status of all connections in this manager. Checks the ping timer and removes the connection if
	 * the partner did not respond in time
	 */
	public abstract void updateConnectionStatus();
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * A {@link CoderThreadPool} that is used for encoding packets
	 * </p>
	 * @return A {@link CoderThreadPool} that is used for encoding packets
	 */
	@Override
	public CoderThreadPool.Encoder getEncoderPool() {
		return encoderPool;
	}
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * A {@link CoderThreadPool} that is used for decoding packets
	 * </p>
	 * @return A {@link CoderThreadPool} that is used for decoding packets
	 */
	@Override
	public CoderThreadPool.Decoder getDecoderPool() {
		return decoderPool;
	}
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * A {@link PacketToByteConverter} that is configured with the configs of this manager
	 * </p>
	 * @return A {@link PacketToByteConverter} used by this manager's connections
	 */
	@Override
	@Internal
	public PacketToByteConverter createToByteConverter() {
		return commonToByteConverter.get();
	}
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * A {@link ByteToPacketConverter} that is configured with the configs of this manager
	 * </p>
	 * @return A {@link ByteToPacketConverter} used by this manager's connections
	 */
	@Override
	@Internal
	public ByteToPacketConverter createToPacketConverter() {
		return commonToPacketConverter.get();
	}
	
	private static final AtomicInteger NAME_INDEX = new AtomicInteger(0);
	static String generateNetworkIdName(String prefix) {
		return prefix + NAME_INDEX.getAndIncrement();
	}
	
}
