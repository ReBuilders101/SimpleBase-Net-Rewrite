package dev.lb.simplebase.net.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.manager.AcceptorThreadDeathReason;

/**
 * A {@link Thread} subclass that reads from a {@link DatagramSocket} until it is interrupted
 */
public class DatagramReceiverThread extends Thread {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("server-accept");
	private static final AtomicInteger THREAD_ID = new AtomicInteger(0);
	
	private final DatagramSocket socket;
	private final Consumer<AcceptorThreadDeathReason> deathReasonHandler;
	private final BiConsumer<InetSocketAddress, ByteBuffer> dataHandler;
	private final ByteBuffer receiveBuffer;

	@Deprecated
	public DatagramReceiverThread(DatagramSocket socket, BiConsumer<InetSocketAddress, ByteBuffer> receiveData,
			Consumer<AcceptorThreadDeathReason> deathReasonHandler, int bufferSize) {
		super("DatagramSocketReceiverThread-" + THREAD_ID.getAndIncrement());
		this.socket = socket;
		this.dataHandler = receiveData;
		this.deathReasonHandler = deathReasonHandler;
		this.receiveBuffer = ByteBuffer.allocate(bufferSize);
	}
	
	public DatagramReceiverThread(DatagramChannel channel, BiConsumer<InetSocketAddress, ByteBuffer> dataHandler,
			Consumer<AcceptorThreadDeathReason> threadDeathHandler, int receiveBufferSize) {
		super("DatagramSocketReceiverThread-" + THREAD_ID.getAndIncrement());
		if(!channel.isBlocking()) throw new IllegalArgumentException("Channel must be in blocking mode");
		
		this.socket = channel.socket();
		this.dataHandler = dataHandler;
		this.deathReasonHandler = threadDeathHandler;
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
		} finally {
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