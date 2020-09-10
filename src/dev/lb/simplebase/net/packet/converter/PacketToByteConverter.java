package dev.lb.simplebase.net.packet.converter;

import java.nio.ByteBuffer;
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
	private final int bufferSize;
	
	/**
	 * Creates a new instance
	 * @param provider Provides {@link PacketIDMapping}s to look up numerical packet IDs
	 * @param destination The next stage that sends bytes through a connection
	 */
	public PacketToByteConverter(PacketIDMappingProvider provider, int bufferSize) {
		this.provider = provider;
		this.bufferSize = bufferSize;
	}
	
	public <Data> ByteBuffer convert(NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider, Data> format, Data data) {
		final ByteBuffer buffer = format.encode(provider, data, bufferSize);
		if(buffer != null) {
			ByteBuffer toSend = ByteBuffer.allocate(buffer.remaining() + 4);
			ByteDataHelper.cInt(format.getUniqueIdentifier(), toSend);
			toSend.put(buffer);
			toSend.flip();
			return toSend;
		} else {
			LOGGER.debug("Format (%s) produced an invalid packet for data %s", format.getName(), data);
			return null;
		}
	}
	
	
	/**
	 * This is stored here because you can't have a private/package static member in interfaces (WHY?)<br>
	 * Belongs to {@link SingleConnectionAdapter}.
	 */
	static final AbstractLogger CONNECTION_LOGGER = NetworkManager.getModuleLogger("connection-receive");
}
