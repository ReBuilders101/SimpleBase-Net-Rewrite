package dev.lb.simplebase.net;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.event.EventDispatcher;
import dev.lb.simplebase.net.events.PacketRejectedEvent;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;


/**
 * This {@link PacketHandler} accepts packets from any number of threads and hands them to
 * the delegate on a single thread available through {@link #getOutputThread()}. 
 */
@Threadsafe
public class PacketThreadReceiver implements PacketHandler {

	private volatile AtomicReference<PacketHandler> delegate;
	private final LinkedBlockingQueue<Runnable> threadTasks; //this implementation is threadsafe
	private final Consumer<PacketRejectedEvent> rejectHandler;
	private final int maxSize;
	private final DelegateThread thread;
	
	/**
	 * Creates a new {@link PacketThreadReceiver} instance. The maximum queue size will be {@link Integer#MAX_VALUE}.<br>
	 * Creating this instance will cause a new daemon {@link Thread} to start.
	 * @param delegate A reference to the {@link PacketHandler} that should receive all packets on one thread. May be updated at any time.
	 * @param rejectHandler The handler created by {@link EventDispatcher#postTask(EventAccessor)} that handles rejected packets
	 */
	public PacketThreadReceiver(AtomicReference<PacketHandler> delegate, Consumer<PacketRejectedEvent> rejectHandler) {
		this(delegate, rejectHandler, Integer.MAX_VALUE);	
	}
	
	/**
	 * Creates a new {@link PacketThreadReceiver} instance.<br>
	 * Creating this instance will cause a new daemon {@link Thread} to start.
	 * @param delegate A reference to the {@link PacketHandler} that should receive all packets on one thread. May be updated at any time.
	 * @param rejectHandler The handler created by {@link EventDispatcher#postTask(EventAccessor)} that handles rejected packets
	 * @param maxQueueSize The maximum size for the queue that holds unprocessed packets
	 */
	public PacketThreadReceiver(AtomicReference<PacketHandler> delegate, Consumer<PacketRejectedEvent> rejectHandler, int maxQueueSize) {
		this.rejectHandler = rejectHandler;
		this.delegate = delegate;
		this.threadTasks = new LinkedBlockingQueue<>(maxQueueSize);
		this.maxSize = maxQueueSize;
		this.thread = new DelegateThread();
		
		thread.start();
	}
	
	/**
	 * The thread that is used to dispatch the {@link Packet}s to the delegate {@link PacketHandler}.
	 * <p>
	 * Interrupting this thread will cause it to end, preventing further packets from being processed.
	 * @return The thread that handles the packets
	 */
	public Thread getOutputThread() {
		return thread;
	}

	/**
	 * The delegate {@link PacketHandler} that receives {@link Packet}s on a single therad.<br>
	 * The current handler can be changed through an {@link AtomicReference} and the returned
	 * handler may be outdated and no longer the currrent one as soon as the method returns
	 * @return
	 */
	public PacketHandler getDelegate() {
		return delegate.get();
	}
	
	/**
	 * The current amount of {@link Packet}s waiting in the queue to be processed
	 * @return The amount of packets in the queue
	 */
	public int getQueueCurrentSize() {
		return threadTasks.size();
	}
	
	/**
	 * The size limit of the queue that stores packets until they are processed
	 * @return
	 */
	public int getQueueMaxSize() {
		return maxSize;
	}
	
	@Override
	public void handlePacket(Packet packet, PacketContext context) {
		//Generate the task that calls the delegate
		final Runnable postTask = () -> delegate.get().handlePacket(packet, context);
		boolean success = threadTasks.offer(postTask);
		if(!success) {
			//This seems like some overhead for the connection thread, but the queue is full anyways, so we take our time
			final PacketRejectedEvent event = new PacketRejectedEvent(context.getRemoteID(), packet.getClass());
			rejectHandler.accept(event);
			if(!event.isCancelled()) {
				NetworkManager.NET_LOG.warning("Incoming Packet rejected: Queue full");
			} else {
				NetworkManager.NET_LOG.debug("Incoming Packet rejected: Queue full");
			}
		}
	}
	
	private static final AtomicInteger THREAD_ID = new AtomicInteger(0);
	@Internal
	private class DelegateThread extends Thread {
		
		private DelegateThread() {
			super("PacketThreadReceiver-DelegateThread-" + THREAD_ID.getAndIncrement());
			setDaemon(true); //Don't wait for queued packets when the rest of the application is dead
		}
		
		@Override
		public void run() {
			while(!Thread.interrupted()) { //End the thread on interrupt
				try {
					final Runnable nextTask = threadTasks.take();
					nextTask.run();
				} catch (InterruptedException e) {
					break;
				}
			}
			NetworkManager.NET_LOG.info("Thread '" + getName() + "' was interrupted and is closing");
			//Thread dies here, on purpose
		}
		
	}
	
}