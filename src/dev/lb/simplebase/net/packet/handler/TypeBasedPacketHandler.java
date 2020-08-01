package dev.lb.simplebase.net.packet.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;

/**
 * Forwards packets to different handlers depending on their implementation class.
 * <p>
 * Every {@link TypeBasedPacketHandler} has two phases:<br>
 * In the registartion phase, {@link #canRegisterHandlers()} is {@code true} and
 * new handlers can be added using the {@link #registerHandler(Class, TypedPacketHandler)} method.<br>
 * In the second phase (locked phase), {@link #canRegisterHandlers()} will immediately return {@code false}
 * and no more handlers can be added.
 * <p>
 * The {@code TypeBasedPacketHandler} starts in phase 1. It will switch to the
 * second phase when {@link #lock()} is called or when the first packet is received
 * (when {@link #handlePacket(Packet, PacketContext)} is first called).<br>
 * Once it is in phase 2, it can never go back to phase 1.
 */
@Threadsafe
public class TypeBasedPacketHandler implements PacketHandler {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("packet-handler");
	
	private final Map<Class<? extends Packet>, PacketHandler> handlers;
	private final AtomicReference<PacketHandler> defaultHandler;
	
	/**
	 * Can change from true to false, but never back to true
	 */
	private volatile boolean locked = false;
	private final Object lockSync = new Object();
	
	/**
	 * Creates a new {@link TypeBasedPacketHandler}.<br>
	 * The instance will have no registered handlers.
	 * The default fallback handler can be changed at any time using the 
	 * {@link AtomicReference#set(Object)} method.
	 * @param The default handler that receives a {@link Packet} when no handler was registered for its type
	 */
	public TypeBasedPacketHandler(AtomicReference<PacketHandler> defaultHandler) {
		this.handlers = new HashMap<>();
		this.defaultHandler = defaultHandler;
	}
	
	/**
	 * Creates a new {@link TypeBasedPacketHandler}.<br>
	 * The instance will have no registered handlers.
	 * The default fallback handler can not be changed later.
	 * @param The default handler that receives a {@link Packet} when no handler was registered for its type
	 */
	public TypeBasedPacketHandler(PacketHandler defaultHandler) {
		this(new AtomicReference<>(defaultHandler));
	}
	
	/**
	 * Creates a new {@link TypeBasedPacketHandler}.<br>
	 * The instance will have no registered handlers. The default fallback handler will
	 * print a warning message with details about the packet type and origin.
	 */
	public TypeBasedPacketHandler() {
		this((packet, context) -> {
			LOGGER.warning("Received unexpected packet type %s from %s", packet.getClass().getName(), context.getRemoteID());
		});
	}
	
	/**
	 * Adds a new {@link TypedPacketHandler} to handle a specific {@link Packet} implementation.<p>
	 * If a handler for that packet class already exists, it cannot be overwritten and this method will return {@code false}.<br>
	 * If the object is already in second phase (see {@link TypeBasedPacketHandler} documentation), this method 
	 * will return {@code false} immediately.
	 * @param <T> The type of {@link Packet} implementation
	 * @param packetType The class object of the type of {@link Packet} implementation
	 * @param handler The {@link TypedPacketHandler} that can accept this type of packet
	 * @return Whether the handler was added successfully
	 */
	public <T extends Packet> boolean registerHandler(Class<T> packetType, TypedPacketHandler<T> handler) {
		Objects.requireNonNull(packetType, "'packetType' parameter must not be null");
		Objects.requireNonNull(handler, "'handler' parameter must not be null");
		
		if(locked) {
			return false;
		} else {
			synchronized (lockSync) {
				if(locked) {
					return false;
				} else {
					return registerHandlerImpl(packetType, handler);
				}
			}
		}
	}
	
	/**
	 * Only call this when synchronized on lockSync
	 * @param packetType not null
	 * @param handler not null
	 * @return success
	 */
	private boolean registerHandlerImpl(Class<? extends Packet> packetType, PacketHandler handler) {
		if(handlers.containsKey(packetType)) {
			return false; //Don't overwrite an existing handler
		} else {
			handlers.put(packetType, handler);
			return true;
		}
	}
	
	/**
	 * {@code True}, if a handler can be registered (phase 1), {@code false} otherwise (phase 2)
	 * <p>
	 * The returned value is immediately outdated as another thread might have locked this object
	 * afterwards. To ensure/check whether a hander was registered successfull, use the return value
	 * of {@link #registerHandler(Class, TypedPacketHandler)} instead.
	 * @return Whether a handler could have been registered at the time this method was called
	 * @see TypeBasedPacketHandler
	 */
	public boolean canRegisterHandlers() {
		if(locked) {
			return false;
		} else {
			synchronized (lockSync) {
				return !locked;
			}
		}
	}
	
	/**
	 * Locks this {@link TypeBasedPacketHandler}.
	 * <p>
	 * Will switch from phase 1 to phase 2. After this method has been called,
	 * no more handlers can be added.<br>
	 * Has no effect if this object is already locked.
	 */
	public void lock() {
		if(!locked) {
			synchronized (lockSync) {
				locked = true;
			}
		}
	}

	@Override
	public void handlePacket(Packet packet, PacketContext context) {
		lock();
		handlers.getOrDefault(packet.getClass(), defaultHandler.get()).handlePacket(packet, context);
	}
}
