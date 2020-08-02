package dev.lb.simplebase.net.manager;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.event.EventAccessor;
import dev.lb.simplebase.net.event.EventDispatchChain;
import dev.lb.simplebase.net.event.EventDispatcher;
import dev.lb.simplebase.net.events.ConnectionClosedEvent;
import dev.lb.simplebase.net.events.PacketSendingFailedEvent;
import dev.lb.simplebase.net.events.PacketReceiveRejectedEvent;
import dev.lb.simplebase.net.events.UnknownConnectionlessPacketEvent;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.packet.PacketIDMapping;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.handler.EmptyPacketHandler;
import dev.lb.simplebase.net.packet.handler.PacketHandler;
import dev.lb.simplebase.net.packet.handler.ThreadPacketHandler;

/**
 * The base class for both server and client managers.
 */
@Threadsafe
public abstract class NetworkManagerCommon {
	
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
	 */
	public final EventAccessor<PacketReceiveRejectedEvent> PacketReceiveRejected;
	
	/**
	 * The {@link UnknownConnectionlessPacketEvent} will be posted when a packet over a UDP connection
	 * or a similar protocol that doesn't maintain a stable connection was received from a source
	 * that didn't register with a login packet before
	 */
	public final EventAccessor<UnknownConnectionlessPacketEvent> UnknownConnectionlessPacket;
	
	
	
	private final NetworkID local;
	private final CommonConfig config;
	private final PacketIDMappingProvider provider;
	private final AtomicReference<PacketHandler> singleThreadHandler;
	private final ThreadPacketHandler multiThreadHandler;
	private final EventDispatcher dispatcher;
	private final Optional<Thread> managedThread;
	
	/**
	 * Constructor that initializes {@link NetworkManagerCommon} base features
	 * @param local The local {@link NetworkID} representing this manager
	 * @param config The configuration object to create this manager. Will be locked if it is not already.
	 */
	protected NetworkManagerCommon(NetworkID local, CommonConfig config) {
		Objects.requireNonNull(local, "'local' parameter must not be null");
		Objects.requireNonNull(config, "'config' parameter must not be null");
		config.lock(); //To be sure, now we can use config without caching the values
		
		this.local = local;
		this.config = config; //It is now locked and can't be changed, so it can be stored
		this.provider = new PacketIDMappingProvider();
		
		//EVENTS
		ConnectionClosed = new EventAccessor<>(ConnectionClosedEvent.class);
		PacketSendingFailed = new EventAccessor<>(PacketSendingFailedEvent.class);
		PacketReceiveRejected = new EventAccessor<>(PacketReceiveRejectedEvent.class);
		UnknownConnectionlessPacket = new EventAccessor<>(UnknownConnectionlessPacketEvent.class);
		
		dispatcher = new EventDispatcher(() -> getLocalID().getDescription());
		singleThreadHandler = new AtomicReference<>(new EmptyPacketHandler());
		if(config.getUseManagedThread()) {
			multiThreadHandler = new ThreadPacketHandler(singleThreadHandler, 
					EventDispatchChain.P2(dispatcher, PacketReceiveRejected, 
					(packet, context) -> new PacketReceiveRejectedEvent(context.getRemoteID(), packet.getClass())));
			managedThread = Optional.of(multiThreadHandler.getOutputThread());
		} else {
			multiThreadHandler = null;
			managedThread = Optional.empty();
		}
		if(config.getGlobalConnectionCheck()) {
			NetworkManager.InternalAccess.INSTANCE.registerManagerForConnectionStatusCheck(this);
		}
	}
	
	/**
	 * The container for {@link PacketIDMapping}s that are used to convert the packets sent form this manager to bytes.
	 */
	public final PacketIDMappingProvider getMappingContainer() {
		return provider;
	}
	
	
	/**
	 * Add a {@link PacketHandler} that receives incoming packets from all connections.<br>
	 * Depending on whether this manager uses a managed thread, the handlers may receive the packets
	 * on only one thread (no thread safety in handlers needed) or on a different thread for every receiver
	 * (thread safety of handlers required). See {@link #getManagedThread()}.
	 * <p>
	 * If more than one handler is added, all handlers receive the same packet in no fixed order.<br>
	 * In some implementations handling might be faster if only a single handler is added.
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
	 * The amount of unprocessed packets. If no managed thread is used, this method always returns 0.<br>
	 * @return The amount of unprocessed packets.
	 */
	public int getPacketQueueSize() {
		return multiThreadHandler == null ? 0 : multiThreadHandler.getQueueCurrentSize();
	}
	
	/**
	 * <b>Internal.</b> Use {@link NetworkConnection#receivePacket(Packet)}
	 * to simulate a received packet instead.<p>
	 * Push a packet received by a connection to this manager.
	 * The manager can accept packets on any thread and will handle it correctly.
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
	 * Returns the PacketSource matching the source ID for connectionless
	 * protocols like UDP that don't know their own PacketSource when received
	 * @param source The source ID of the packet
	 * @return The corresponding context, or {@code null} if the source is not registered
	 */
	@Internal
	protected abstract PacketContext getConnectionlessPacketContext(NetworkID source);
	
	/**
	 * <b>Internal only.</b> Using this can leave connections in a broken state where 
	 * handler threads might not be closed.
	 * Removes the connection with the proper synchronization, without causing any other
	 * side effects like closing the connection or posting an event to the dispatcher
	 */
	@Internal
	public	abstract void removeConnectionSilently(NetworkConnection connection);
	
	/**
	 * The event dispatcher. Only API-internal code may post events
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
	public CommonConfig getConfig() {
		return config;
	}
	
	/**
	 * An array of all events that this network manager implementation can post.<br>
	 * Useful combined with {@link EventAccessor#addAllHandlers(Class, EventAccessor...)} to
	 * register handlers for all events on this manager.
	 * <p>
	 * The returned array <b>must not be modified</b>.
	 * @return
	 */
	public abstract EventAccessor<?>[] getEvents();
	
	/**
	 * Cleans up all resources and connections held by this manager. Should be called manually before
	 * the manager is left to garbage collection.
	 */
	public void cleanUp() {
		managedThread.ifPresent(Thread::interrupt);
		NetworkManager.InternalAccess.INSTANCE.unregisterManagerForConnectionStatusCheck(this);
	}
	
	/**
	 * Updates the status of all connections in this manager. Pings the remote partner and removes the connection if
	 * the partner does not respond
	 */
	public abstract void updateConnectionStatus();
	
}
