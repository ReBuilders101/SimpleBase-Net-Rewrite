package dev.lb.simplebase.net.packet.converter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.IntFunction;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.manager.NetworkManagerProperties;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormat;

/**
 * Converts bytes to packets.
 * <br><b>Not Threadsafe!</b><br>
 * Call {@code accept...} methods from only one thread.
 */
public final class ByteToPacketConverter {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("packet-decode");
	
	private final PacketIDMappingProvider provider;
	private final IntFunction<ByteInflater> chooseInflater;

	public ByteToPacketConverter(NetworkManagerProperties managerLike) {
		this.provider = managerLike.getMappingContainer();
		this.chooseInflater = makeFunction(managerLike.getConfig().getCompressionSize());
	}
	
	private static IntFunction<ByteInflater> makeFunction(int minSize) {
		if(minSize < 0) {
			return i -> ByteInflater.NO_COMPRESSION;
		} else {
			return i -> i < minSize ? ByteInflater.NO_COMPRESSION : ByteInflater.ZIP_COMPRESSION_PREFIXED;
		}
	}
	
	public void convertAndPublish(ByteBuffer data, NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider, ?> format, ConnectionAdapter adapter) {
		try {
			ByteBuffer uncompressed = decompress(data, format);
			format.decodeAndPublish(adapter, provider, uncompressed);
		} catch (IOException e) {
			LOGGER.error("cannot inflate packet data", e);
		}
	}
	
	private ByteBuffer decompress(ByteBuffer raw, NetworkPacketFormat<?, ?, ?> format) throws IOException {
		if(format.supportsCompression()) {
			ByteInflater decompressor = chooseInflater.apply(raw.remaining());
			return decompressor.inflate(raw);
		} else {
			return raw;
		}
	}
}
