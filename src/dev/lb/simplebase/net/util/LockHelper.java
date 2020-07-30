package dev.lb.simplebase.net.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Utility methods to deal with different lock implementations
 */
public class LockHelper {

	/**
	 * Checks whether a lock is held by the calling thread. Not all {@link Lock} implementations support this
	 * operation.<p>
	 * <b>Supported types:</b>
	 * <ul><li>{@link ReentrantLock}</li><li>{@link ReentrantReadWriteLock.WriteLock}</li></ul>
	 * @param lock The lock to check
	 * @param defaultBehavior The value to return when the lock implementation is not supported
	 * @return Whether the lock holds the thread, or the default value
	 */
	public static boolean isHeldByCurrentThread(Lock lock, boolean defaultBehavior) {
		if(lock instanceof ReentrantLock) {
			return ((ReentrantLock) lock).isHeldByCurrentThread();
		} else if(lock instanceof ReentrantReadWriteLock.WriteLock) {
			return ((ReentrantReadWriteLock.WriteLock) lock).isHeldByCurrentThread();
		} else {
			return defaultBehavior;
		}
	}
	
	/**
	 * Checks whether a lock is held by the calling thread. Not all {@link Lock} implementations support this
	 * operation.<p>
	 * <b>Supported types:</b>
	 * <ul><li>{@link ReentrantLock}</li><li>{@link ReentrantReadWriteLock.WriteLock}</li></ul>
	 * @param lock The lock to check
	 * @return Whether the lock holds the thread
	 * @throws UnsupportedOperationException When the lock implementation is not supported
	 */
	public static boolean isHeldByCurrentThread(Lock lock) {
		if(lock instanceof ReentrantLock) {
			return ((ReentrantLock) lock).isHeldByCurrentThread();
		} else if(lock instanceof ReentrantReadWriteLock.WriteLock) {
			return ((ReentrantReadWriteLock.WriteLock) lock).isHeldByCurrentThread();
		} else {
			throw new UnsupportedOperationException(lock.getClass().getName() + ": Lock implementation does not support isHeldByCurrentThread()");
		}
	}
	
}
