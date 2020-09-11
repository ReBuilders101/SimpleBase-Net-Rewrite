package dev.lb.simplebase.net.packet.converter;

import java.nio.ByteBuffer;
import java.util.function.IntFunction;

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
	private final IntFunction<ByteDeflater> chooseDeflater;
	
	/**
	 * Creates a new instance
	 * @param provider Provides {@link PacketIDMapping}s to look up numerical packet IDs
	 * @param destination The next stage that sends bytes through a connection
	 */
	public PacketToByteConverter(PacketIDMappingProvider provider, int bufferSize, int compressionSize) {
		this.provider = provider;
		this.bufferSize = bufferSize;
		this.chooseDeflater = makeFunction(compressionSize);
	}
	
	private static IntFunction<ByteDeflater> makeFunction(int minSize) {
		if(minSize < 0) {
			return i -> ByteDeflater.NO_COMPRESSION;
		} else {
			return i -> i < minSize ? ByteDeflater.NO_COMPRESSION : ByteDeflater.ZIP_COMPRESSION_PREFIXED;
		}
	}
	
	public <Data> ByteBuffer convert(NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider, Data> format, Data data) {
		final ByteBuffer rawPacketData = format.encode(provider, data, bufferSize);
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
	
	private ByteBuffer compress(ByteBuffer raw, NetworkPacketFormat<?, ? , ?> format) {
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
	static final AbstractLogger CONNECTION_LOGGER = NetworkManager.getModuleLogger("connection-receive");
}
