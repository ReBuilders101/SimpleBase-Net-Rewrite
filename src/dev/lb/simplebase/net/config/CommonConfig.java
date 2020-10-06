package dev.lb.simplebase.net.config;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import dev.lb.simplebase.net.GlobalTimer;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.annotation.ValueType;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.io.WritableByteData;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.handler.PacketHandler;
import dev.lb.simplebase.net.util.Cloneable2;

/**
 * <p>
 * The {@link CommonConfig} class holds the states of all config options that can be applied to a {@link NetworkManagerCommon} instance at creation time.
 * These options are applicable to both clients and servers.
 * </p>
 * <h2>Thread safety</h2>
 * <p>
 * The {@link CommonConfig} class and both subclasses included with the API ({@link ClientConfig} and {@link ServerConfig}) are all threadsafe.
 * If the synchronization object is the {@code CommonConfig} object itself. {@code get...()} methods are single reads and never acquire the lock,
 * as those operations will be executed frequently while the program is running. Getters do not require locking because it is impossible to leave
 * a {@code CommonConfig} object in an invalid state.<br>
 * <b>Mutating a config object on multiple threads should not be necessary:</b> Despite being possible, it is rarely necessary or appropiate to
 * modify the config on more than one thread. It is best practise to initialize all values and then <i>lock</i> the config object to prevent further mutation.
 * </p>
 * <h2>Locking Behavior</h2>
 * <p>
 * To ensure that config objects and their properties can be cached anywhere in a {@link NetworkManagerCommon} and related classes the {@link CommonConfig}
 * class includes a special <i>locking</i> mechanism. After a {@code CommonConfig} instance has been locked, all attempts to set a config value
 * will result in an {@link IllegalStateException}. A locked config can never be unlocked, but a new instance that has the same values without being locked can
 * be created using the copy constructor. Config objects can be locked by calling {@link #lock()}.
 * </p>
 * <h2>Comparison and Cloning</h2>
 * <p>
 * The {@link #equals(Object)} method compares all configured values in the two instances. <b>It does not consider the <i>locked</i> state of either config.</b>
 * A locked and a different unlocked {@code CommonConfig} object are considered equal as long as they store the same configuration values and produce equally configured
 * network managers. To comply with the contract for the {@code equals} and {@code hashCode} methods, {@link #hashCode()} also does not consider the <i>locked</i> state
 * when calculating the hash.<br>
 * The locked state will be copied when using the {@link #clone()} method.
 * </p>
 */
@ValueType
@Threadsafe
public class CommonConfig implements Cloneable2 {

	/**
	 * Can be used in {@link #setConnectionCheckTimeout(int)} to disable the timeout.
	 * A connection check will never be considered failed when the timeout is disabled.
	 */
	public static final int DISABLE_CONNECTION_TIMEOUT = -1;
	/**
	 * Can be used in {@link #setCompressionSize(int)} to disable packet compression entirely.
	 * All packets will be sent uncompressed, regardless of their size.
	 */
	public static final int DISABLE_COMPRESSION = -1;
	
	//DEFAULT VALUES HERE
	private static final int PACKET_BUFFER_INITIAL_SIZE = 4096;
	private static final int CONNECTION_CHECK_TIMEOUT = 5000;
	private static final boolean USE_HANDLER_THREAD = true;
	private static final boolean GLOBAL_CONNECTION_CHECK = false;
	private static final int COMPRESSION_SIZE = DISABLE_COMPRESSION;
	private static final boolean USE_ENCODER_POOL = true;
	private static final int DATAGRAM_PACKET_SIZE = 4096;
	private static final boolean USE_DECODER_POOL = true;
	
	//Only this one needs to be up-to-date everywhere immediately
	private volatile boolean locked;
	
	private boolean useHandlerThread;
	private int packetBufferInitialSize;
	private int connectionCheckTimeout;
	private boolean globalConnectionCheck;
	private int compressionSize;
	private boolean useEncoderPool;
	private int datagramPacketSize;
	private boolean useDecoderPool;
	
	/**
	 * <p>
	 * Creates a new {@link CommonConfig} instance with default configuration values. The instance will not be <i>locked</i>.
	 * </p>
	 * <table>
	 * <caption>Config option defaults:</caption>
	 * <tr><th>Getter method name</th><th>Initial value</th></tr>
	 * <tr><td>{@link #getUseHandlerThread()}</td><td>{@code true}<td></tr>
	 * <tr><td>{@link #getPacketBufferInitialSize()}</td><td>{@code 4096}</td></tr>
	 * <tr><td>{@link #getConnectionCheckTimeout()}</td><td>{@code 5000} [ms]</td></tr>
	 * <tr><td>{@link #getGlobalConnectionCheck()}</td><td>{@code false}</td></tr>
	 * <tr><td>{@link #getCompressionSize()}</td><td>{@link #DISABLE_COMPRESSION}</td></tr>
	 * <tr><td>{@link #getDatagramPacketMaxSize()}</td><td>{@code 4096}</td></tr>
	 * <tr><td>{@link #getUseEncoderThreadPool()}</td><td>{@code true}</td></tr>
	 * <tr><td>{@link #getUseDecoderThreadPool()}</td><td>{@code true}</td></tr>
	 * </table>
	 */
	public CommonConfig() {
		this.useHandlerThread = USE_HANDLER_THREAD;
		this.packetBufferInitialSize = PACKET_BUFFER_INITIAL_SIZE;
		this.connectionCheckTimeout = CONNECTION_CHECK_TIMEOUT;
		this.compressionSize = COMPRESSION_SIZE;
		this.useEncoderPool = USE_ENCODER_POOL;
		this.datagramPacketSize = DATAGRAM_PACKET_SIZE;
		this.useDecoderPool = USE_DECODER_POOL;
		this.globalConnectionCheck = GLOBAL_CONNECTION_CHECK;
		
		this.locked = false;
	}
	
	/**
	 * Creates a new {@link CommonConfig} instance that copies all configuration values from a different {@code CommonConfig} object.
	 * The created instance will not be <i>locked</i>.<br>
	 * The constructor will synchronize on the template object while copying values to prevent concurrent modification. 
	 * @param template The old {@link CommonConfig} that holds the configuration values
	 */
	public CommonConfig(CommonConfig template) {
		synchronized (template) {
			this.useHandlerThread = template.getUseHandlerThread();
			this.packetBufferInitialSize = template.getPacketBufferInitialSize();
			this.connectionCheckTimeout = template.getConnectionCheckTimeout();
			this.compressionSize = template.getCompressionSize();
			this.useEncoderPool = template.getUseEncoderThreadPool();
			this.datagramPacketSize = template.getDatagramPacketMaxSize();
			this.useDecoderPool = template.getUseDecoderThreadPool();
			this.globalConnectionCheck = template.getGlobalConnectionCheck();
			this.locked = false;
		}
	}
	
	/**
	 * The maximum size of a datagram packet sent through a connection of {@link ConnectionType#UDP}.
	 * <p>
	 * This is not the maximum size of a logical {@link Packet} for UDP connections. If a logical packet
	 * is larger than this config value after converting it to bytes, it will simply be split into several
	 * UDP packets.
	 * </p><p>
	 * The main purpose of this config value is to set the size of the UDP receive buffer, which is not growable and
	 * allocated for every connection.
	 * </p>
	 * @return The maximum byte size of a datagram packet sent by this API
	 */
	public int getDatagramPacketMaxSize() {
		return datagramPacketSize;
	}
	
	/**
	 * Sets the maximum size of a datagram packet sent through a connection of {@link ConnectionType#UDP}.
	 * <p>
	 * This is not the maximum size of a logical {@link Packet} for UDP connections. If a logical packet
	 * is larger than this config value after converting it to bytes, it will simply be split into several
	 * UDP packets.
	 * </p><p>
	 * The main purpose of this config value is to set the size of the UDP receive buffer, which is not growable and
	 * allocated for every connection.
	 * </p>
	 * @param value The new value for this config option
	 * @return {@code this}
	 * @throws IllegalStateException When this config object is locked
	 */
	public synchronized CommonConfig setDatagramPacketMaxSize(int value) throws IllegalStateException {
		checkLocked();
		this.datagramPacketSize = value;
		return this;
	}
	
	/**
	 * Determines whether to use a thread pool to encode packets to bytes.
	 * <p>
	 * Depending on the size of the packet, the complexity of the packet's data structures and the
	 * implementation of {@link Packet#writeData(dev.lb.simplebase.net.io.WritableByteData)}, encoding a
	 * packet can take different amounts of time. Delegating the task of encoding to a thread pool ensures
	 * that the call to {@link NetworkConnection#sendPacket(Packet)} (or a corresponding method on one of the
	 * network managers) is consistently fast.<br>
	 * If disabled, packets are encoded on the thread that calls the method sending the packet. IO operations
	 * (writing to the socket's stream / channel) also happen on the thread that encodes the packets.
	 * </p><p>
	 * The used thread pool is an {@link Executors#newCachedThreadPool()}.
	 * </p>
	 * @return Whether to use a thread pool for packet encoding
	 */
	public boolean getUseEncoderThreadPool() {
		return useEncoderPool;
	}
	
	/**
	 * Sets whether to use a thread pool to encode packets to bytes.
	 * <p>
	 * Depending on the size of the packet, the complexity of the packet's data structures and the
	 * implementation of {@link Packet#writeData(dev.lb.simplebase.net.io.WritableByteData)}, encoding a
	 * packet can take different amounts of time. Delegating the task of encoding to a thread pool ensures
	 * that the call to {@link NetworkConnection#sendPacket(Packet)} (or a corresponding method on one of the
	 * network managers) is consistently fast.<br>
	 * If disabled, packets are encoded on the thread that calls the method sending the packet. IO operations
	 * (writing to the socket's stream / channel) also happen on the thread that encodes the packets.
	 * </p><p>
	 * The used thread pool is an {@link Executors#newCachedThreadPool()}.
	 * </p>
	 * @param value The new value for this config option
	 * @return {@code this}
	 * @throws IllegalStateException When this config object is locked
	 */
	public synchronized CommonConfig setUseEncoderThreadPool(boolean value) throws IllegalStateException {
		checkLocked();
		this.useEncoderPool = value;
		return this;
	}
	
	/**
	 * Determines whether to use a thread pool to decode bytes to packets.
	 * <p>
	 * Depending on the size of the packet, the complexity of the packet's data structures and the
	 * implementation of {@link Packet#readData(dev.lb.simplebase.net.io.ReadableByteData)}, decoding a
	 * packet can take different amounts of time. Delegating the task of decoding to a thread pool ensures
	 * that the thread that reads from the connection is not blocked or overloaded with decoding and
	 * network data can be received without delays and backlogs<br>
	 * If disabled, packets are decoded on the thread that reads from the socket's stream / channel / selector.
	 * In case of a UDP or NIO server, this is a single thread for all connections, which can become overloaded
	 * when many packets arrive simultaneously.  
	 * </p><p>
	 * The used thread pool is an {@link Executors#newCachedThreadPool()}.
	 * </p>
	 * @return Whether to use a thread pool for packet decoding
	 */
	public boolean getUseDecoderThreadPool() {
		return useDecoderPool;
	}
	
	/**
	 * Sets whether to use a thread pool to decode bytes to packets.
	 * <p>
	 * Depending on the size of the packet, the complexity of the packet's data structures and the
	 * implementation of {@link Packet#readData(dev.lb.simplebase.net.io.ReadableByteData)}, decoding a
	 * packet can take different amounts of time. Delegating the task of decoding to a thread pool ensures
	 * that the thread that reads from the connection is not blocked or overloaded with decoding and
	 * network data can be received without delays and backlogs<br>
	 * If disabled, packets are decoded on the thread that reads from the socket's stream / channel / selector.
	 * In case of a UDP or NIO server, this is a single thread for all connections, which can become overloaded
	 * when many packets arrive simultaneously.  
	 * </p><p>
	 * The used thread pool is an {@link Executors#newCachedThreadPool()}.
	 * </p>
	 * @param value The new vaule for this config option
	 * @return {@code this}
	 * @throws IllegalStateException When this config object is locked
	 */
	public synchronized CommonConfig setUseDecoderThreadPool(boolean value) throws IllegalStateException {
		checkLocked();
		this.useDecoderPool = value;
		return this;
	}
	
	/**
	 * Common setter for {@link #setUseEncoderThreadPool(boolean)} and {@link #setUseDecoderThreadPool(boolean)}.
	 * <p>Both config options are frequently set to the same value. In those cases, this convenience method can be used.</p>
	 * @param value The value for both config options
	 * @return {@code this}
	 * @throws IllegalStateException When this config object is locked
	 */
	public synchronized CommonConfig setUseThreadPools(boolean value) throws IllegalStateException {
		checkLocked();
		this.useEncoderPool = value;
		this.useDecoderPool = value;
		return this;
	}
	
	/**
	 * The minimum byte size for a packet to be compressed before sending them.
	 * <p>
	 * For very large packets, compressing them can save network bandwidth, but requires additional time for
	 * encoding and decoding. With this option, a packet size limit can be set that determines whether it is more effective to
	 * send the entire packet uncompressed or to compress it.
	 * </p><p>
	 * Negative values mean that packets will always be sent uncompressed.<br>
	 * Compression and decompression is done using the {@link Deflater} and {@link Inflater} classes.
	 * </p>
	 * @return The minimum byte size for a packet to be compressed
	 */
	public int getCompressionSize() {
		return compressionSize;
	}
	
	/**
	 * Sets the minimum byte size for a packet to be compressed before sending them.
	 * <p>
	 * For very large packets, compressing them can save network bandwidth, but requires additional time for
	 * encoding and decoding. With this option, a packet size limit can be set that determines whether it is more effective to
	 * send the entire packet uncompressed or to compress it.
	 * </p><p>
	 * Set to {@link CommonConfig#DISABLE_COMPRESSION} to disable packet compression entirely.<br>
	 * Compression and decompression is done using the {@link Deflater} and {@link Inflater} classes.
	 * </p>
	 * @param value The new value for this config option
	 * @return {@code this}
	 * @throws IllegalStateException When this config object is locked
	 */
	public synchronized CommonConfig setCompressionSize(int value) throws IllegalStateException {
		checkLocked();
		this.compressionSize = value;
		return this;
	}
	
	/**
	 * The timeout value for connection checks (in milliseconds).
	 * <p>
	 * When {@link NetworkConnection#checkConnection()} is called, the start time of the ping will be stored.
	 * The next time {@link NetworkConnection#updateConnectionStatus()} is called, the connection will be closed
	 * if pinging took longer than the configured timeout value.
	 * </p><p>
	 * To automatically close a connection when the timeout is reached, set the {@link #getGlobalConnectionCheck()}
	 * option to {@code true}.
	 * </p>
	 * @return The maximum timeout for a connection ping
	 */
	public int getConnectionCheckTimeout() {
		return connectionCheckTimeout;
	}
	
	/**
	 * Sets the timeout value for connection checks (in milliseconds).
	 * <p>
	 * When {@link NetworkConnection#checkConnection()} is called, the start time of the ping will be stored.
	 * The next time {@link NetworkConnection#updateConnectionStatus()} is called, the connection will be closed
	 * if pinging took longer than the configured timeout value.
	 * </p><p>
	 * To automatically close a connection when the timeout is reached, set the {@link #getGlobalConnectionCheck()}
	 * option to {@code true}.
	 * </p>
	 * @param value The new value for this config option 
	 * @return {@code this}
	 * @throws IllegalStateException When this config object is locked
	 */
	public synchronized CommonConfig setConnectionCheckTimeout(int value) throws IllegalStateException {
		checkLocked();
		this.connectionCheckTimeout = value;
		return this;
	}
	
	/**
	 * Determines whether the {@link NetworkManagerCommon} will have a dedicated thread for packet handlers.
	 * <p>
	 * Incoming {@link Packet}s can arrive at the {@link PacketHandler} from a number of different threads, such as
	 * TCP receiver threads, UDP receiver threads and Selector threads, as well as the threads in the decoder
	 * pool if {@link #getUseDecoderThreadPool()} is set to {@code true}. This can make it harder to write threadsafe
	 * packet handler code.
	 * </p><p>
	 * If this option is enabled, {@link NetworkManagerCommon#getManagedThread()} will contain a single thread on which all
	 * packet handlers will be executed, regardless of packet origin. This way, handlers only ever run on one thread and never in
	 * parallel. Can be disabled if a single handler thread is not fast enough to process all packets
	 * </p>
	 * @return Whether to use a dedicated packet handler thread
	 */
	public boolean getUseHandlerThread() {
		return useHandlerThread;
	}
	
	/**
	 * Sets whether the {@link NetworkManagerCommon} will have a dedicated thread for packet handlers.
	 * <p>
	 * Incoming {@link Packet}s can arrive at the {@link PacketHandler} from a number of different threads, such as
	 * TCP receiver threads, UDP receiver threads and Selector threads, as well as the threads in the decoder
	 * pool if {@link #getUseDecoderThreadPool()} is set to {@code true}. This can make it harder to write threadsafe
	 * packet handler code.
	 * </p><p>
	 * If this option is enabled, {@link NetworkManagerCommon#getManagedThread()} will contain a single thread on which all
	 * packet handlers will be executed, regardless of packet origin. This way, handlers only ever run on one thread and never in
	 * parallel. Can be disabled if a single handler thread is not fast enough to process all packets
	 * </p>
	 * @param value The new value for this config option
	 * @return {@code this}
	 * @throws IllegalStateException When this config object is locked 
	 */
	public synchronized CommonConfig setUseHandlerThread(boolean value) throws IllegalStateException {
		checkLocked();
		this.useHandlerThread = value;
		return this;
	}
	
	/**
	 * The initial size of the dynamic accumulator buffers for packet encoding/decoding.
	 * <p>
	 * This determines the initial size of several growable buffers used during encoding and decoding:
	 * </p>
	 * <ul>
	 * <li>The buffer that backs the {@link WritableByteData} when encoding packets, as long as the packet does
	 * not provide its own fixed size</li>
	 * <li>The buffer that accumulates received bytes until a packet is completely received</li>
	 * <li>The buffer used for channel reads of NIO TCP connections</li>
	 * </ul>
	 * <p>
	 * The size of the UDP receive buffer is controlled by a different config option: {@link #getDatagramPacketMaxSize()}.
	 * </p>
	 * @return The initial size for growable buffers
	 */
	public int getPacketBufferInitialSize() {
		return packetBufferInitialSize;
	}
	
	/**
	 * Sets the initial size of the dynamic accumulator buffers for packet encoding/decoding.
	 * <p>
	 * This determines the initial size of several growable buffers used during encoding and decoding:
	 * </p>
	 * <ul>
	 * <li>The buffer that backs the {@link WritableByteData} when encoding packets, as long as the packet does
	 * not provide its own fixed size</li>
	 * <li>The buffer that accumulates received bytes until a packet is completely received</li>
	 * <li>The buffer used for channel reads of NIO TCP connections</li>
	 * </ul>
	 * <p>
	 * The size of the UDP receive buffer is controlled by a different config option: {@link #getDatagramPacketMaxSize()}.
	 * </p>
	 * @param value The new value for this config option
	 * @return {@code this}
	 * @throws IllegalStateException When this config is locked
	 */
	public synchronized CommonConfig setPacketBufferInitialSize(int value) throws IllegalStateException {
		checkLocked();
		this.packetBufferInitialSize = value;
		return this;
	}
	
	/**
	 * Whether to subscribe the {@link NetworkManagerCommon} to the global connection checker thread.
	 * <p>
	 * If enabled, a thread managed by the {@link GlobalTimer} class periodically calls
	 * {@link NetworkConnection#updateConnectionStatus()} on all of the manager's connections. This will
	 * automatically remove a connection that has timed out after a call to {@link NetworkConnection#checkConnection()}.
	 * </p>
	 * @return Whether to subscribe to the global connection checket thread
	 */
	public boolean getGlobalConnectionCheck() {
		return globalConnectionCheck;
	}
	
	/**
	 * Sets whether to subscribe the {@link NetworkManagerCommon} to the global connection checker thread.
	 * <p>
	 * If enabled, a thread managed by the {@link GlobalTimer} class periodically calls
	 * {@link NetworkConnection#updateConnectionStatus()} on all of the manager's connections. This will
	 * automatically remove a connection that has timed out after a call to {@link NetworkConnection#checkConnection()}.
	 * </p>
	 * @param value The new value for this config option
	 * @return {@code this}
	 * @throws IllegalStateException When this config is locked
	 */
	public synchronized CommonConfig setGlobalConnectionCheck(boolean value) throws IllegalStateException {
		checkLocked();
		this.globalConnectionCheck = value;
		return this;
	}
	
	/**
	 * Internally used in setters. Throws an {@link IllegalStateException} if the object is locked.
	 * Only call from synchronized methods.
	 */
	@Internal
	protected void checkLocked() {
		if(locked) throw new IllegalStateException("CommonConfig object is locked and cannot be altered");
	}
	
	/**
	 * Locks this config object.
	 * <p>
	 * After a config has been locked, all setter methods will throw an {@link IllegalStateException} and
	 * no longer alter the configuration values, making this config object effectively immutable.
	 * </p>
	 * <p>
	 * Calling this method on a locked config object has no effect.
	 * </p>
	 * @see #isLocked()
	 */
	public synchronized void lock() {
		locked = true;
	}
	
	/**
	 * Whether this config object is locked.
	 * <p>
	 * After a config has been locked, all setter methods will throw an {@link IllegalStateException} and
	 * no longer alter the configuration values, making this config object effectively immutable.
	 * </p>
	 * @return {@code true} if the object is locked and immutable, {@code false} if it can still be altered
	 * @see #lock()
	 */
	public synchronized boolean isLocked() {
		return locked;
	}

	@Override
	public synchronized CommonConfig clone() {
		try {
			return (CommonConfig) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Cannot clone Config object");
		}
	}

	@Override
	public synchronized CommonConfig copy() {
		if(locked) {
			return this;
		} else {
			return clone();
		}
	}
	
	@Override
	public String toString() {
		return "CommonConfig [locked=" + locked + ", useManagedThread=" + useHandlerThread
				+ ", encodeBufferInitialSize=" + packetBufferInitialSize + ", connectionCheckTimeout="
				+ connectionCheckTimeout + ", globalConnectionCheck=" + globalConnectionCheck + ", compressionSize="
				+ compressionSize + ", useEncoderPool=" + useEncoderPool + ", datagramPacketSize=" + datagramPacketSize
				+ ", useDecoderPool=" + useDecoderPool + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(compressionSize, connectionCheckTimeout, datagramPacketSize, packetBufferInitialSize,
				globalConnectionCheck, useDecoderPool, useEncoderPool, useHandlerThread);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof CommonConfig)) {
			return false;
		}
		CommonConfig other = (CommonConfig) obj;
		return compressionSize == other.compressionSize && connectionCheckTimeout == other.connectionCheckTimeout
				&& datagramPacketSize == other.datagramPacketSize
				&& packetBufferInitialSize == other.packetBufferInitialSize
				&& globalConnectionCheck == other.globalConnectionCheck && useDecoderPool == other.useDecoderPool
				&& useEncoderPool == other.useEncoderPool && useHandlerThread == other.useHandlerThread;
	}
}
