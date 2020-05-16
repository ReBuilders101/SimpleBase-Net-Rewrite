package dev.lb.simplebase.net.config;

import dev.lb.simplebase.net.NetworkManagerCommon;
import dev.lb.simplebase.net.PacketHandler;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;

/**
 * Sets config values for {@link NetworkManagerCommon} on creation.<p>
 * Instances can be reused after they have been used to create a manager,
 * but they should be mutated on one thread only if possible.<br>
 * If the config object should be used on multiple threads (e.g. as a global config for all
 * managers, it is best practise to initialize all values once and the call {@link #lock()},
 * which prevents further modification.
 */
@Threadsafe
public abstract class CommonConfig implements Cloneable {

	/**
	 * Can be used in {@link #setConnectionCheckTimeout(int)} to disable
	 * the timeout (makes connections wait indefinitely).
	 */
	public static int DISABLE_CONNECTION_TIMEOUT = -1;
	
	private static final int BUFFER_INITIAL_DEFAULT = 128;
	private static final int CONNECTION_CHECK_DEFAULT = 1000;
	private static final boolean USE_MANAGED_DEFAULT = true;
	
	//Only this one needs to be up-to-date everywhere immediately
	private volatile boolean locked;
	
	private boolean useManagedThread;
	private int encodeBufferInitialSize;
	private int connectionCheckTimeout;
	
	/**
	 * Creates a new CommonConfig instance. Instance will not be locked
	 * <p>
	 * Initial values are:
	 * <table>
	 * <tr><th>Getter method name</th><th>Initial value</th></tr>
	 * <tr><td>{@link #getUseManagedThread()}</td><td>{@value #USE_MANAGED_DEFAULT}<td></tr>
	 * <tr><td>{@link #getEncodeBufferInitialSize()}</td><td>{@value #BUFFER_INITIAL_DEFAULT}</td></tr>
	 * <tr><td>{@link #getConnectionCheckTimeout()}</td><td>{@value #CONNECTION_CHECK_DEFAULT}</td></tr>
	 * </table>
	 */
	protected CommonConfig() {
		this.useManagedThread = USE_MANAGED_DEFAULT;
		this.encodeBufferInitialSize = BUFFER_INITIAL_DEFAULT;
		this.connectionCheckTimeout = CONNECTION_CHECK_DEFAULT;
		this.locked = false;
	}
	
	/**
	 * The connection check timeout is the time in milliseconds that the connection
	 * will wait before it is treated as disconnected.
	 * @return The connection check timeout in milliseconds
	 */
	public synchronized int getConnectionCheckTimeout() {
		return connectionCheckTimeout;
	}
	
	/**
	 * The connection check timeout is the time in milliseconds that the connection
	 * will wait before it is treated as disconnected.
	 * @param value The new value for the connection timeout in milliseconds
	 * @throws IllegalStateException If this config object is locked ({@link #isLocked()})
	 */
	public synchronized void setConnectionCheckTimeout(int value) {
		checkLocked();
		this.connectionCheckTimeout = value;
	}
	
	/**
	 * If a managed thread is used, all {@link PacketHandler}s registered with the manager will
	 * be called on a single thread, otherwise they will be called on the connections data receiver
	 * thread (See {@link NetworkManagerCommon#getManagedThread()} for more details).
	 * @param value The new value for whether to use the managed thread in a manager
	 * @throws IllegalStateException If this config object is locked ({@link #isLocked()})
	 */
	public synchronized void setUseManagedThread(boolean value) {
		checkLocked();
		this.useManagedThread = value;
	}
	
	/**
	 * If a managed thread is used, all {@link PacketHandler}s registered with the manager will
	 * be called on a single thread, otherwise they will be called on the connections data receiver
	 * thread (See {@link NetworkManagerCommon#getManagedThread()} for more details).
	 * @return {@code true} if the managed thread will be used in a manager, {@code false} otherwise
	 */
	public synchronized boolean getUseManagedThread() {
		return useManagedThread;
	}
	
	/**
	 * The size in bytes that the buffer used to encode packets will have when
	 * created. The buffer is able to dynamically expand if it is to small, but this takes
	 * additional time. If the buffer is to large, memory space is wasted.
	 * @param value The new value for the initial size of the packet encode buffer
	 * @throws IllegalStateException If this config object is locked ({@link #isLocked()})
	 */
	public synchronized void setEncodeBufferInitialSize(int value) {
		checkLocked();
		this.encodeBufferInitialSize = value;
	}
	
	/**
	 * The size in bytes that the buffer used to encode packets will have when
	 * created. The buffer is able to dynamically expand if it is to small, but this takes
	 * additional time. If the buffer is to large, memory space is wasted.
	 * @return The initial size of the packet encode buffer
	 */
	public synchronized int getEncodeBufferInitialSize() {
		return encodeBufferInitialSize;
	}
	
	/**
	 * Internally used in setters. Throws an {@link IllegalStateException} if the object is locked.
	 */
	@Internal
	protected synchronized void checkLocked() {
		if(locked) throw new IllegalStateException("CommonConfig object is locked and cannot be altered");
	}
	
	/**
	 * Locks this object. After locking, all setter methods will throw
	 * an {@link IllegalStateException} instead of changing the state.<br>
	 * After locking, the object will be completely thread safe.
	 * <p>
	 * If the object is already locked, calling this again has no effect
	 */
	public synchronized void lock() {
		locked = true;
	}
	
	/**
	 * Whether this object is locked and cannot be altered. Locked instances
	 * will throw an {@link IllegalStateException} when a setter is called instead of
	 * changing the state.
	 * @return {@code true} if the object is locked and immutable, {@code false} if it can still be altered
	 * @see #lock()
	 */
	public synchronized boolean isLocked() {
		return locked;
	}
	
}
