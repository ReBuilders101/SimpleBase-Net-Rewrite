package dev.lb.simplebase.net.packet.converter;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public interface MutableAddressConnectionAdapter extends ConnectionAdapter {

	public void setAddress(InetSocketAddress address);
	
	public ReferenceCounter getUseCountManager();
	
	
	public static class ReferenceCounter {
		
		private final AtomicInteger counter = new AtomicInteger(0);
		
		public void acquire() {
			counter.getAndIncrement();
		}
		
		public void release() {
			if(counter.decrementAndGet() < 0) {
				throw new IllegalStateException("ReferenceCounter: invalid release() - counter was already 0");
			}
		}
		
		public int getCounter() {
			return counter.get();
		}
	}
}
