package dev.lb.simplebase.net.connection;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import dev.lb.simplebase.net.event.EventDispatchChain;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;

public class EncoderThreadPool {

	private final ExecutorService service;
	private final EventDispatchChain.P1<RejectedExecutionException, ?> rejectedHandler;
	private final boolean useEncoderPool;
	
	public EncoderThreadPool(NetworkManagerCommon manager, EventDispatchChain.P1<RejectedExecutionException, ?> rejectedHandler) {
		this.service = Executors.newCachedThreadPool(new MarkedThreadFactory());
		this.rejectedHandler = rejectedHandler;
		this.useEncoderPool = manager.getConfig().getUseEncoderThreadPool();
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
	
	public void encodeAndSendPacket(ExternalNetworkConnection connection, Packet packet) {
		try {
			service.submit(() -> {	
				connection.sendPacket(packet);
			});
		} catch (RejectedExecutionException e) {
			rejectedHandler.post(e);
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
			super(runnable, "EncoderPool-" + poolId + "-Thread-" + threadId.getAndIncrement());
			
			if (isDaemon()) setDaemon(false);
            if (getPriority() != Thread.NORM_PRIORITY) setPriority(Thread.NORM_PRIORITY);
		}
		
		private EncoderThreadPool getPool() {
			return EncoderThreadPool.this;
		}
	}
}
