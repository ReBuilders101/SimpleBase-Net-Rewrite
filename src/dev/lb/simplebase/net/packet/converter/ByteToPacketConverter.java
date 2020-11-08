package dev.lb.simplebase.net.packet.converter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.IntFunction;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.log.Logger;
import dev.lb.simplebase.net.manager.NetworkManagerProperties;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormat;

/**
 * Converts bytes to packets.
 * <br><b>Not Threadsafe!</b><br>
 * Call {@code accept...} methods from only one thread.
 */
public final class ByteToPacketConverter {
	static final Logger LOGGER = NetworkManager.getModuleLogger("packet-decode");
	
	private final PacketIDMappingProvider provider;
	private final IntFunction<ByteInflater> chooseInflater;

	/**
	 * Creates a new {@link ByteToPacketConverter}.
	 * @param managerLike The {@link NetworkManagerProperties} holding configs
	 */
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
	
	/**
	 * Converts the content of the buffer into a network packet and sends it to the adapter
	 * @param data The {@link ByteBuffer} with the complete packet data
	 * @param format The {@link NetworkPacketFormat} to use for decoding
	 * @param adapter The {@link ConnectionAdapter} that will receive the network packet
	 */
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
