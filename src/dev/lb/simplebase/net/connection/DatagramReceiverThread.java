package dev.lb.simplebase.net.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.manager.AcceptorThreadDeathReason;

/**
 * A {@link Thread} subclass that reads from a {@link DatagramSocket} or 
 * blocking {@link DatagramChannel} until it is interrupted.
 * <p>
 * When interrupted, the thread will close the associated socket or channel and return from its {@link #run()} method.
 * Before the terminating it will call the {@code threadDeathHandler} set in the constructor with {@link AcceptorThreadDeathReason#INTERRUPTED}.<br>
 * Interrrupting is considered the 'regular' way to shut down the thread.
 * </p><p>
 * The {@code threadDeathHandler} will also be called before the thread terminates for any other reason;
 * see {@link AcceptorThreadDeathReason}'s enum constants for more details.
 * </p>
 */
public class DatagramReceiverThread extends Thread {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("server-accept");
	private static final AtomicInteger THREAD_ID = new AtomicInteger(0);
	
	private final DatagramSocket socket;
	private final Consumer<AcceptorThreadDeathReason> deathReasonHandler;
	private final BiConsumer<InetSocketAddress, ByteBuffer> dataHandler;
	private final ByteBuffer receiveBuffer;

	/**
	 * Creates a new {@link DatagramReceiverThread} based on a {@link DatagramSocket}.
	 * <p>
	 * The thread will not be started by the constructor, that must be done by calling {@link #start()} at a later time.
	 * </p>
	 * @deprecated It is recommended to use a blocking channel instead
	 * @param socket The {@code DatagramSocket} that will receive the data
	 * @param receiveData The function that will process received packets
	 * @param deathReasonHandler The function that is called when this thread terminates
	 * @param bufferSize The size of the datagram receive buffer, and the maximum size for incoming packets
	 * @throws NullPointerException When any of the parameters is {@code null}
	 * @throws IllegalArgumentException When the {@code bufferSize} is a negative integer
	 */
	@Deprecated
	public DatagramReceiverThread(DatagramSocket socket, BiConsumer<InetSocketAddress, ByteBuffer> receiveData,
			Consumer<AcceptorThreadDeathReason> deathReasonHandler, int bufferSize) {
		super("DatagramReceiverThread-" + THREAD_ID.getAndIncrement());
		
		this.socket = Objects.requireNonNull(socket, "'socket' parameter must not be null");
		this.dataHandler = Objects.requireNonNull(receiveData, "'receiveData' parameter must not be null");
		this.deathReasonHandler = Objects.requireNonNull(deathReasonHandler, "'deathReasonHandler' parameter must not be null");
		this.receiveBuffer = ByteBuffer.allocate(bufferSize);
	}
	
	/**
	 * Creates a new {@link DatagramReceiverThread} based on a {@link DatagramChannel}.
	 * <p>
	 * The thread will not be started by the constructor, that must be done by calling {@link #start()} at a later time.
	 * </p>
	 * @param threadNameDetail A short description of the thread's role that will be included in the thread name; May be {@code null}
	 * @param channel The blocking {@code DatagramChannel} that will receive the data
	 * @param dataHandler The function that will process received packets
	 * @param threadDeathHandler The function that is called when this thread terminates
	 * @param receiveBufferSize The size of the datagram receive buffer, and the maximum size for incoming packets
	 * @throws NullPointerException When any of the parameters except {@code threadNameDetail} is {@code null}
	 * @throws IllegalArgumentException When the channel is in non-blocking mode, or when {@code receiveBufferSize} is a negative integer
	 */
	public DatagramReceiverThread(String threadNameDetail, DatagramChannel channel, BiConsumer<InetSocketAddress, ByteBuffer> dataHandler,
			Consumer<AcceptorThreadDeathReason> threadDeathHandler, int receiveBufferSize) {
		super("DatagramReceiverThread-" + THREAD_ID.getAndIncrement() + 
				(threadNameDetail == null ? "" : " (" + threadNameDetail + ")"));
		if(!channel.isBlocking()) throw new IllegalArgumentException("Channel must be in blocking mode");
		
		this.socket = Objects.requireNonNull(channel, "'channel' parameter must not be null").socket();
		this.dataHandler = Objects.requireNonNull(dataHandler, "'dataHandler' parameter must not be null");
		this.deathReasonHandler = Objects.requireNonNull(threadDeathHandler, "'threadDeathHandler' parameter must not be null");
		this.receiveBuffer = ByteBuffer.allocate(receiveBufferSize);
	}

	@Override
	public void run() {
		final DatagramPacket receivePacket = new DatagramPacket(receiveBuffer.array(), receiveBuffer.capacity());
		AcceptorThreadDeathReason deathReason = AcceptorThreadDeathReason.UNKNOWN;
		LOGGER.info("UDP server socket listener thread started");;
		try {
			while(!this.isInterrupted()) {
				try {
					//TODO switch from sockets to blocking channels to avoid this hack
					socket.receive(receivePacket); //Actually reads into the backing array and not the buffer...
					receiveBuffer.limit(receivePacket.getLength());//...so the limit must be adjusted manually
					receiveBuffer.rewind();
					dataHandler.accept(getAddress(receivePacket), receiveBuffer);
					receiveBuffer.clear();
				} catch (SocketException e) {
					deathReason = AcceptorThreadDeathReason.INTERRUPTED;
				} catch (IOException e) {
					deathReason = AcceptorThreadDeathReason.IOEXCEPTION;
				}

			}
		} finally { //This will always be called before the thread dies, even for any unchecked exception.
			deathReasonHandler.accept(deathReason);
			LOGGER.info("UDP server socket listener thread ended with reason %s", deathReason);	
		}
	}

	private static InetSocketAddress getAddress(DatagramPacket packet) {
		if(packet.getSocketAddress() instanceof InetSocketAddress) {
			return (InetSocketAddress) packet.getSocketAddress();
		} else {
			return new InetSocketAddress(packet.getAddress(), packet.getPort());
		}
	}

	@Override
	public void interrupt() {
		super.interrupt();
		if(isInterrupted() && !socket.isClosed()) {
			socket.close();
		}
	}

}
