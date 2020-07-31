package dev.lb.simplebase.net.packet.converter;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.io.ByteDataHelper;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.packet.PacketIDMapping;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormat;

/**
 * Converts packets to bytes
 */
public final class PacketToByteConverter {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("packet-encode");
	
	private final PacketIDMappingProvider provider;
	private final Consumer<ByteBuffer> destination;
	
	/**
	 * Creates a new instance
	 * @param provider Provides {@link PacketIDMapping}s to look up numerical packet IDs
	 * @param destination The next stage that sends bytes through a connection
	 */
	public PacketToByteConverter(PacketIDMappingProvider provider, Consumer<ByteBuffer> destination) {
		this.provider = provider;
		this.destination = destination;
	}

	/**
	 * Converts the data into bytes using the provided format and sends the bytes to the next stage
	 * @param <Data> The type of data
	 * @param format The encoding format
	 * @param data The data to encode
	 */
	public <Data> void convertAndPublish(NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider, Data> format, Data data) {
		final ByteBuffer buffer = format.encode(provider, data);
		if(buffer != null) {
			ByteBuffer toSend = ByteBuffer.allocate(buffer.remaining() + 4);
			ByteDataHelper.cInt(format.getUniqueIdentifier(), toSend);
			toSend.put(buffer);
			destination.accept(buffer);
		} else {
			LOGGER.debug("Format (%s) produced an invalid packet for data %s", format.getName(), data);
		}
	}
	
	
	/**
	 * This is stored here because you can't have a private/package static member in interfaces (WHY?)<br>
	 * Belongs to {@link TcpConnectionAdapter}.
	 */
	static final AbstractLogger CONNECTION_LOGGER = NetworkManager.getModuleLogger("connection-receive");
}
