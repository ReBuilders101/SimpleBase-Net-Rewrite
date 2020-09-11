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
	private final NetworkManagerCommon manager;
	private final EventDispatchChain.P1<RejectedExecutionException, ?> rejectedHandler;
	
	public EncoderThreadPool(NetworkManagerCommon manager, EventDispatchChain.P1<RejectedExecutionException, ?> rejectedHandler) {
		this.service = Executors.newCachedThreadPool(new MarkedThreadFactory());
		this.manager = manager;
		this.rejectedHandler = rejectedHandler;
	}
	
	public boolean isValidEncoderThread(NetworkManagerCommon manager) {
		//If no pool is used, we can encode on any thread
		if(!manager.getConfig().getUseEncoderThreadPool()) return true;
		
		final Thread thread = Thread.currentThread();
		if(thread instanceof MarkedThread) {
			final MarkedThread mth = (MarkedThread) thread;
			return mth.getManager() == manager;
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
		
		private NetworkManagerCommon getManager() {
			return EncoderThreadPool.this.manager;
		}
	}
}
