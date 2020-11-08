package dev.lb.simplebase.net.packet.converter;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ConnectionAdapter} that stores a {@link InetSocketAddress}.
 */
public interface MutableAddressConnectionAdapter extends ConnectionAdapter {

	/**
	 * Sets the {@link InetSocketAddress} stored with this adapterrrr
	 * @param address The new address to store
	 */
	public void setAddress(InetSocketAddress address);
	
	/**
	 * A {@link ReferenceCounter} that counts the amount of references to this adapter.
	 * @return A {@link ReferenceCounter} for this adapter
	 */
	public ReferenceCounter getUseCountManager();
	
	/**
	 * Maintains an atomic counter of all refereces made to an object.
	 */
	public static class ReferenceCounter {
		
		private final AtomicInteger counter = new AtomicInteger(0);
		
		/**
		 * Increases the counter by one.
		 */
		public void acquire() {
			counter.getAndIncrement();
		}
		
		/**
		 * Decreases the counter by one.
		 * If the counter is already zero, an {@link IllegalStateException} is thrown.
		 */
		public void release() {
			if(counter.decrementAndGet() < 0) {
				throw new IllegalStateException("ReferenceCounter: invalid release() - counter was already 0");
			}
		}
		
		/**
		 * The current amount of references.
		 * @return The current amount of references
		 */
		public int getCounter() {
			return counter.get();
		}
	}
}
