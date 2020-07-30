package dev.lb.simplebase.net.packet.handler;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.util.LockHelper;
import dev.lb.simplebase.net.util.ThreadsafeIterable;

/**
 * A {@link PacketHandler} that maintains a list of other handlers.<br>
 */
@Threadsafe
public final class MultiPacketHandler implements PacketHandler {

	//Will be asked to iterate from many different threads at the same time
	private final ReadWriteLock lockHandlers;
	private final List<PacketHandler> handlers;	
	
	private final Threadsafe readThreadsafe;
	private final Threadsafe writeThreadsafe;
	
	public MultiPacketHandler() {
		//List dynamically expands, only used operations are add and iterate, so LinkedList is good
		handlers = new LinkedList<>();
		lockHandlers = new ReentrantReadWriteLock();
		readThreadsafe  = new Threadsafe(lockHandlers.readLock());
		writeThreadsafe = new Threadsafe(lockHandlers.writeLock());
	}
	
	/**
	 * Adds a {@link PacketHandler} to the list of handlers.
	 * There is no check for duplicates: if a handler is added twice, it will
	 * receive every packet twice.
	 * @param handler The new handler
	 */
	public void addHandler(PacketHandler handler) {
		try {
			lockHandlers.writeLock().lock();
			handlers.add(handler);
		} finally {
			lockHandlers.writeLock().unlock();
		}
	}
	
	@Override
	public void handlePacket(Packet packet, PacketContext context) {
		try {
			lockHandlers.readLock().lock();
			for(PacketHandler handler : handlers) {
				handler.handlePacket(packet, context);
			}
		} finally {
			lockHandlers.readLock().unlock();
		}
	}

	public ThreadsafeIterable<MultiPacketHandler, PacketHandler> exclusiveThreadsafe() {
		return writeThreadsafe;
	}
	
	public ThreadsafeIterable<MultiPacketHandler, PacketHandler> readOnlyThreadsafe() {
		return readThreadsafe;
	}
	
	private final class Threadsafe implements ThreadsafeIterable<MultiPacketHandler, PacketHandler> {
		private final Lock lock;
		
		private Threadsafe(Lock lock) {
			this.lock = lock;
		}
		
		@Override
		public void action(Consumer<MultiPacketHandler> action) {
			try {
				lock.lock();
				action.accept(MultiPacketHandler.this);
			} finally {
				lock.unlock();
			}
		}

		@Override
		public <R> R actionReturn(Function<MultiPacketHandler, R> action) {
			try {
				lock.lock();
				return action.apply(MultiPacketHandler.this);
			} finally {
				lock.unlock();
			}
		}

		@Override
		public void forEach(Consumer<? super PacketHandler> itemAction) {
			try {
				lock.lock();
				handlers.forEach(itemAction);
			} finally {
				lock.unlock();
			}
		}

		@Override
		public Iterator<PacketHandler> iterator() {
			if(LockHelper.isHeldByCurrentThread(lock, true)) { //If it is held by this thread then it is held somewhere up the stack -> 
				return handlers.iterator(); //so we can return an instance to the caller (relatively) safely
			} else {
				throw new IllegalStateException("Current thread does not hold exclusive lock"); //No lock, no iterator
			}
		}

		@Override
		public Spliterator<PacketHandler> spliterator() {
			if(LockHelper.isHeldByCurrentThread(lock, true)) { //If it is held by this thread then it is held somewhere up the stack -> 
				return handlers.spliterator(); //so we can return an instance to the caller (relatively) safely
			} else {
				throw new IllegalStateException("Current thread does not hold exclusive lock"); //No lock, no iterator
			}
		}

		@Override
		public <R> Optional<R> forEachReturn(Function<? super PacketHandler, Optional<R>> itemFunction) {
			try {
				lock.lock();
				for(PacketHandler handler : handlers) {
					Optional<R> val = itemFunction.apply(handler);
					if(val.isPresent()) return val;
				}
				return Optional.empty();
			} finally {
				lock.unlock();
			}
		}
		
	}
	
}
