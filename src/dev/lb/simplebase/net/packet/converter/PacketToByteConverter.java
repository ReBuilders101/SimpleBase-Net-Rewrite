package dev.lb.simplebase.net.packet.converter;

import java.nio.ByteBuffer;
import java.util.function.IntFunction;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.io.ByteDataHelper;
import dev.lb.simplebase.net.log.Logger;
import dev.lb.simplebase.net.manager.NetworkManagerProperties;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormat;

/**
 * Converts packets to bytes
 */
public final class PacketToByteConverter {
	static final Logger LOGGER = NetworkManager.getModuleLogger("packet-encode");
	
	private final NetworkManagerProperties managerLike;
	private final int bufferSize;
	private final IntFunction<ByteDeflater> chooseDeflater;
	
	/**
	 * Creates a new instance.
	 * @param managerLike The {@link NetworkManagerProperties} that holds necessary configs and mappings
	 */
	public PacketToByteConverter(NetworkManagerProperties managerLike) {
		this.managerLike = managerLike;
		this.bufferSize = managerLike.getConfig().getPacketBufferInitialSize();
		this.chooseDeflater = makeFunction(managerLike.getConfig().getCompressionSize());
	}
	
	private static IntFunction<ByteDeflater> makeFunction(int minSize) {
		if(minSize < 0) {
			return i -> ByteDeflater.NO_COMPRESSION;
		} else {
			return i -> i < minSize ? ByteDeflater.NO_COMPRESSION : ByteDeflater.ZIP_COMPRESSION_PREFIXED;
		}
	}
	
	/**
	 * Convert a network packet to bytes.
	 * @param format The {@link NetworkPacketFormat} to use for encoding
	 * @param data The payload data for the packet
	 * @return The {@link ByteBuffer} with the encoded packet
	 */
	public final <Data> ByteBuffer convert(NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider, Data> format, Data data) {
		
		final ByteBuffer rawPacketData = format.encode(managerLike.getMappingContainer(), data, bufferSize);
		if(rawPacketData != null) {
			ByteBuffer compressedPacketData = compress(rawPacketData, format);
			
			ByteBuffer completeData = ByteBuffer.allocate(compressedPacketData.remaining() + 4);
			ByteDataHelper.cInt(format.getUniqueIdentifier(), completeData);
			completeData.put(compressedPacketData);
			completeData.flip();
			return completeData;
		} else {
			LOGGER.debug("Format (%s) produced an invalid packet for data %s", format.getName(), data);
			return null;
		}
	}
	
	private ByteBuffer compress(ByteBuffer raw, NetworkPacketFormat<?, ?, ?> format) {
		if(format.supportsCompression()) {
			ByteDeflater compressor = chooseDeflater.apply(raw.remaining());
			return compressor.deflate(raw);
		} else {
			return raw;
		}
	}
	
	/**
	 * This is stored here because you can't have a private/package static member in interfaces (WHY?)<br>
	 * Belongs to {@link SingleConnectionAdapter}.
	 */
	static final Logger CONNECTION_LOGGER = NetworkManager.getModuleLogger("connection-receive");
}
