package dev.lb.simplebase.net.connection;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.event.EventDispatchChain;
import dev.lb.simplebase.net.event.EventDispatchChain.P1;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.converter.ByteToPacketConverter;
import dev.lb.simplebase.net.packet.converter.ConnectionAdapter;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormat;
import dev.lb.simplebase.net.util.InternalAccess;

/**
 * A common subclass for {@link CoderThreadPool.Encoder} and {@link CoderThreadPool.Decoder}, which represent the optional
 * encoder and decoder thread pools of a {@link NetworkManagerCommon} that can be enabled in the manager's {@link CommonConfig}.
 */
public abstract class CoderThreadPool {

	protected final ExecutorService service;
	protected final EventDispatchChain.P1<RejectedExecutionException, ?> rejectedHandler;
	private final boolean usePool;
	private final String logPrefix;
	
	/**
	 * Internal Constructor
	 * @param logPrefix Prefix for logging. Set to 'En' or 'De' by the subclass constructor
	 * @param useThisPool Whether this pool was enabled in the configs
	 * @param rejectedHandler What happens when a task could not be accepted by the thread pool
	 */
	protected CoderThreadPool(String logPrefix, boolean useThisPool, EventDispatchChain.P1<RejectedExecutionException, ?> rejectedHandler) {
		InternalAccess.assertCaller(NetworkManagerCommon.class, 1, "Cannot instantiate any type of CoderThreadPool");
		
		this.service = Executors.newCachedThreadPool(new MarkedThreadFactory());
		this.rejectedHandler = rejectedHandler;
		this.usePool = useThisPool;
		this.logPrefix = logPrefix;
	}
	
	/**
	 * Checks whether the calling thread is a valid encoder/decoder thread for this pool.
	 * <p>
	 * If the pool is disabled in the config, this method returns {@code true} for any calling thread.
	 * <p>
	 * @return Whether the calling thread is part of this pool
	 */
	public boolean isValidCoderThread() {
		//If no pool is used, we can encode on any thread
		if(!usePool) return true;
		
		final Thread thread = Thread.currentThread();
		if(thread instanceof MarkedThread) {
			final MarkedThread mth = (MarkedThread) thread;
			return mth.getPool() == this;
		} else {
			return false;
		}
	}
	
	/**
	 * Shuts down the {@link ExecutorService} associated with the pool.
	 * <p>
	 * After this method is called, no more tasks can be scheduled for processing in the pool.
	 * </p><p>
	 * Usually called <b>only</b> from {@link NetworkManagerCommon#cleanUp()} of the associated manager.
	 * </p>
	 */
	@Internal
	public void shutdown() {
		service.shutdown();
	}

	private static final AtomicInteger poolId = new AtomicInteger();
	private static final AtomicInteger threadId = new AtomicInteger();
	
	private final class MarkedThreadFactory implements ThreadFactory {
		private final int localPoolId = poolId.getAndIncrement();
		
		@Override
		public Thread newThread(Runnable r) {
			return new MarkedThread(r, localPoolId);
		}
		
	}
	
	private final class MarkedThread extends Thread {
		
		private MarkedThread(Runnable runnable, int poolId) {
			super(runnable, logPrefix + "coderPool-" + poolId + "-Thread-" + threadId.getAndIncrement());
			
			if (isDaemon()) setDaemon(false);
            if (getPriority() != Thread.NORM_PRIORITY) setPriority(Thread.NORM_PRIORITY);
		}
		
		private CoderThreadPool getPool() {
			return CoderThreadPool.this;
		}
	}
	
	/**
	 * The encoder implementation of {@link CoderThreadPool}.<p>
	 * Features a single method {@link Encoder#encodeAndSendPacket(NetworkConnection, Packet)}
	 * to process a packet on the pool thread.</p>
	 */
	public static final class Encoder extends CoderThreadPool{

		/**
		 * <h2>Internal use only</h2>
		 * <p>
		 * This constructor is used internally by the API and can not be called directly.
		 * </p><hr><p>
		 * Creates a new encoder pool with the requested properties
		 * </p>
		 * @param config The config used to determine whether the encoder pool is active
		 * @param rejectedHandler The handler that processes cases where a task could not be accepted by the pool
		 */
		@Internal
		public Encoder(CommonConfig config, P1<RejectedExecutionException, ?> rejectedHandler) {
			super("En", config.getUseEncoderThreadPool(), rejectedHandler);
		}
		
		/**
		 * Sends the packet to the connection on a thread in the encoder thread pool.
		 * <p>
		 * This method does not check {@link #isValidCoderThread()} - The caller should do that
		 * to determine whether this method has to be called at all. The packet supplied to this method will
		 * be passed on on the thread pool regardless of whether it is active or not.
		 * </p>
		 * @param connection The {@link NetworkConnection} that should send the packet on the thread
		 * @param packet The {@link Packet} to send
		 */
		@Internal
		public void encodeAndSendPacket(NetworkConnection connection, Packet packet) {
			try {
				service.submit(() -> {	
					connection.sendPacket(packet);
				});
			} catch (RejectedExecutionException e) {
				rejectedHandler.post(e);
			}
		}
	}
	
	/**
	 * The decoder implementation of {@link CoderThreadPool}.<p>
	 * Features a single method {@link Decoder#decodeAndSendPacket(ConnectionAdapter, ByteToPacketConverter, NetworkPacketFormat, ByteBuffer)}
	 * to process a packet on the pool thread.</p>
	 */
	public static final class Decoder extends CoderThreadPool{

		/**
		 * <h2>Internal use only</h2>
		 * <p>
		 * This constructor is used internally by the API and can not be called directly.
		 * </p><hr><p>
		 * Creates a new decoder pool with the requested properties
		 * </p>
		 * @param config The config used to determine whether the decoder pool is active
		 * @param rejectedHandler The handler that processes cases where a task could not be accepted by the pool
		 */
		public Decoder(CommonConfig config, P1<RejectedExecutionException, ?> rejectedHandler) {
			super("De", config.getUseDecoderThreadPool(), rejectedHandler);
		}
		
		/**
		 * Converts the {@link ByteBuffer} using the supplied converter and format on a thread of the thread pool
		 * and then sends the decoded object to the {@link ConnectionAdapter}.
		 * <p>
		 * This method does not check {@link #isValidCoderThread()} - The caller should do that
		 * to determine whether this method has to be called at all. The data supplied to this method will
		 * be decoded by the thread pool regardless of whether it is active or not.
		 * </p>
		 * @param connection The {@link ConnectionAdapter} that will receive the decoded message
		 * @param converter The {@link ByteToPacketConverter} that converts the buffer int a packet
		 * @param format The {@link NetworkPacketFormat} used for decoding the buffer
		 * @param data The completely accumulated packet/message to decode and send to the adapter
		 */
		public void decodeAndSendPacket(ConnectionAdapter connection, ByteToPacketConverter converter,
				NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider, ?> format, ByteBuffer data) {
			try {
				service.submit(() -> {	
					converter.convertAndPublish(data, format, connection);
				});
			} catch (RejectedExecutionException e) {
				rejectedHandler.post(e);
			}
		}
	}
	
}
