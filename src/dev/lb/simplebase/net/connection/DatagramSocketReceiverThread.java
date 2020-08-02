package dev.lb.simplebase.net.connection;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public class DatagramSocketReceiverThread extends Thread {

	private final DatagramSocket socket;
	private final BiConsumer<InetSocketAddress, ByteBuffer> dataHandler;
	private final int bufferSize;
	
	public DatagramSocketReceiverThread(DatagramSocket socket, BiConsumer<InetSocketAddress, ByteBuffer> receiveData, int bufferSize) {
		this.socket = socket;
		this.dataHandler = receiveData;
		this.bufferSize = bufferSize;
	}

	@Override
	public void run() {
		while(!this.isInterrupted()) {
			
		}
	}
	
	
}
