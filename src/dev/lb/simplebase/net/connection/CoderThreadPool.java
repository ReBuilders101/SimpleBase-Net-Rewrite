package dev.lb.simplebase.net.connection;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import dev.lb.simplebase.net.event.EventDispatchChain;
import dev.lb.simplebase.net.event.EventDispatchChain.P1;
import dev.lb.simplebase.net.manager.NetworkManagerProperties;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.converter.ByteToPacketConverter;
import dev.lb.simplebase.net.packet.converter.ConnectionAdapter;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormat;

public class CoderThreadPool {

	protected final ExecutorService service;
	protected final EventDispatchChain.P1<RejectedExecutionException, ?> rejectedHandler;
	private final boolean useEncoderPool;
	private final String logPrefix;
	
	protected CoderThreadPool(String logPrefix, NetworkManagerProperties manager, EventDispatchChain.P1<RejectedExecutionException, ?> rejectedHandler) {
		this.service = Executors.newCachedThreadPool(new MarkedThreadFactory());
		this.rejectedHandler = rejectedHandler;
		this.useEncoderPool = manager.getConfig().getUseEncoderThreadPool();
		this.logPrefix = logPrefix;
	}
	
	public boolean isValidEncoderThread() {
		//If no pool is used, we can encode on any thread
		if(!useEncoderPool) return true;
		
		final Thread thread = Thread.currentThread();
		if(thread instanceof MarkedThread) {
			final MarkedThread mth = (MarkedThread) thread;
			return mth.getPool() == this;
		} else {
			return false;
		}
	}
	
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
	
	public static final class Encoder extends CoderThreadPool{

		public Encoder(NetworkManagerProperties manager, P1<RejectedExecutionException, ?> rejectedHandler) {
			super("En", manager, rejectedHandler);
		}
		
		public void encodeAndSendPacket(ExternalNetworkConnection connection, Packet packet) {
			try {
				service.submit(() -> {	
					connection.sendPacket(packet);
				});
			} catch (RejectedExecutionException e) {
				rejectedHandler.post(e);
			}
		}
	}
	
	public static final class Decoder extends CoderThreadPool{

		public Decoder(NetworkManagerProperties manager, P1<RejectedExecutionException, ?> rejectedHandler) {
			super("De", manager, rejectedHandler);
		}
		
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
