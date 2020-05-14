package dev.lb.simplebase.net;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.event.EventAccessor;
import dev.lb.simplebase.net.event.EventDispatcher;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketIDMapping;
import dev.lb.simplebase.net.packet.PacketIDMappingContainer;

/**
 * The base class for both server and client managers.
 */
@Threadsafe
public abstract class NetworkManagerCommon {

	/**
	 * The container for {@link PacketIDMapping}s that are used to convert the packets sent form this manager to bytes.
	 */
	@Threadsafe	public final PacketIDMappingContainer MappingContainer;
	
	/**
	 * The {@link ConnectionClosedEvent} will be posted to this accessor when a connection handled by this manager is
	 * closed.
	 */
	@Threadsafe	public final EventAccessor<ConnectionClosedEvent> ConnectionClosed;
	
	/**
	 * The {@link PacketFailedEvent} will be posted to this accessor when a packet could not
	 * be sent through a connection.
	 */
	@Threadsafe public final EventAccessor<PacketFailedEvent> PacketSendingFailed;
	
	/**
	 * The {@link PacketRejectedEvent} will be posted to this accessor when a packet couldn't be
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
	@Threadsafe public final EventAccessor<PacketRejectedEvent> PacketReceiveRejected;
	
	private final NetworkID local;
	
	private final AtomicReference<PacketHandler> singleThreadHandler;
	private final PacketThreadReceiver multiThreadHandler;
	private final EventDispatcher dispatcher;
	private final Optional<Thread> managedThread;
	
	/**
	 * Constructor that initializes {@link NetworkManagerCommon} base features
	 * @param local The local {@link NetworkID} representing this manager
	 * @param useManagedThread If true, all incoming packets will be handled on a single thread, otherwise on the connection thread
	 */
	protected NetworkManagerCommon(NetworkID local, boolean useManagedThread) {
		this.local = local;
		MappingContainer = new PacketIDMappingContainerImpl();
		ConnectionClosed = new EventAccessor<>(ConnectionClosedEvent.class);
		PacketSendingFailed = new EventAccessor<>(PacketFailedEvent.class);
		PacketReceiveRejected = new EventAccessor<>(PacketRejectedEvent.class);
		
		dispatcher = new EventDispatcher();
		singleThreadHandler = new AtomicReference<>(new EmptyPacketHandler());
		if(useManagedThread) {
			multiThreadHandler = new PacketThreadReceiver(singleThreadHandler, dispatcher.postTask(PacketReceiveRejected));
			managedThread = Optional.of(multiThreadHandler.getOutputThread());
		} else {
			multiThreadHandler = null;
			managedThread = Optional.empty();
		}
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
		singleThreadHandler.getAndUpdate((old) -> PacketHandler.addHandler(old, handler));
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
	 * Push a packet received by a connection to this manager
	 */
	protected void receivePacketOnConnectionThread(Packet packet, PacketContext context) {
		if(managedThread.isPresent()) {
			multiThreadHandler.handlePacket(packet, context);
		} else {
			singleThreadHandler.get().handlePacket(packet, context);
		}
	}
}
