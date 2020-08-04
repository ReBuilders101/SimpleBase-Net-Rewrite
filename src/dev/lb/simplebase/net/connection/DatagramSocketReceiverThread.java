package dev.lb.simplebase.net.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.manager.AcceptorThreadDeathReason;

public class DatagramSocketReceiverThread extends Thread {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("server-accept");
	
	private final DatagramSocket socket;
	private final Consumer<AcceptorThreadDeathReason> deathReasonHandler;
	private final BiConsumer<InetSocketAddress, ByteBuffer> dataHandler;
	private final int bufferSize;

	public DatagramSocketReceiverThread(DatagramSocket socket, BiConsumer<InetSocketAddress, ByteBuffer> receiveData,
			Consumer<AcceptorThreadDeathReason> deathReasonHandler, int bufferSize) {
		this.socket = socket;
		this.dataHandler = receiveData;
		this.deathReasonHandler = deathReasonHandler;
		this.bufferSize = bufferSize;
	}

	@Override
	public void run() {
		final ByteBuffer receiveBuffer = ByteBuffer.allocate(bufferSize);
		final DatagramPacket receivePacket = new DatagramPacket(receiveBuffer.array(), receiveBuffer.capacity());
		AcceptorThreadDeathReason deathReason = AcceptorThreadDeathReason.UNKNOWN;
		LOGGER.info("UDP server socket listener thread started");;
		try {
			while(!this.isInterrupted()) {
				try {
					socket.receive(receivePacket); //SocketException on close
					receiveBuffer.limit(receivePacket.getLength());
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
