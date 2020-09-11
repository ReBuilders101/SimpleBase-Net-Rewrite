package dev.lb.simplebase.net.connection;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import dev.lb.simplebase.net.event.EventDispatchChain;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;

public class EncoderThreadPool {

	private final ExecutorService service;
	private final NetworkManagerCommon manager;
	private final EventDispatchChain.P1<RejectedExecutionException, ?> rejectedHandler;
	
	public EncoderThreadPool(NetworkManagerCommon manager, EventDispatchChain.P1<RejectedExecutionException, ?> rejectedHandler) {
		this.service = Executors.newCachedThreadPool(MarkedThread::new);
		this.manager = manager;
		this.rejectedHandler = rejectedHandler;
	}
	
	public boolean isPooledThreadFor(NetworkManagerCommon manager) {
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
	
	private final class MarkedThread extends Thread {
		
		private MarkedThread(Runnable runnable) {
			super(runnable);
		}
		
		private NetworkManagerCommon getManager() {
			return EncoderThreadPool.this.manager;
		}
	}
}
