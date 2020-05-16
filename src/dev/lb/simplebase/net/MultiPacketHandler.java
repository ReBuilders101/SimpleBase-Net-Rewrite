package dev.lb.simplebase.net;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;

/**
 * A {@link PacketHandler} that maintains a list of other handlers.<br>
 */
@Threadsafe
public final class MultiPacketHandler implements PacketHandler {

	//Will be asked to iterate from many different threads at the same time
	private final ReadWriteLock lockHandlers;
	private final List<PacketHandler> handlers;	
	
	public MultiPacketHandler() {
		//List dynamically expands, only used operations are add and iterate, so LinkedList is good
		handlers = new LinkedList<>();
		lockHandlers = new ReentrantReadWriteLock();
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
	
	
	
}
