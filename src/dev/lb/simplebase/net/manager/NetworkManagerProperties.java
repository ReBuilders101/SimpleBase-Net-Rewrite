package dev.lb.simplebase.net.manager;

import java.util.concurrent.RejectedExecutionException;
import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.connection.CoderThreadPool;
import dev.lb.simplebase.net.event.EventDispatchChain;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.converter.ByteToPacketConverter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;
import dev.lb.simplebase.net.util.Lazy;

/**
 * Holds Properties of a {@link NetworkManagerCommon}, but is not always backet by an actual implmentation
 */
public interface NetworkManagerProperties {

	public CommonConfig<?> getConfig();
	public PacketIDMappingProvider getMappingContainer();
	
	public PacketToByteConverter createToByteConverter();
	public ByteToPacketConverter createToPacketConverter();
	
	public CoderThreadPool.Encoder getEncoderPool();
	public CoderThreadPool.Decoder getDecoderPool();
	
	public static NetworkManagerProperties of(CommonConfig<?> config, PacketIDMappingProvider provider,
			EventDispatchChain.P1<RejectedExecutionException, ?> onEncoderError, EventDispatchChain.P1<RejectedExecutionException, ?> onDecoderError) {
		return new NetworkManagerProperties() {
			private final Lazy<PacketToByteConverter> toByte = new Lazy<>(() -> new PacketToByteConverter(this));
			private final Lazy<ByteToPacketConverter> toPack = new Lazy<>(() -> new ByteToPacketConverter(this));			
			@Override
			public PacketIDMappingProvider getMappingContainer() {
				return provider;
			}
			
			@Override
			public CommonConfig<?> getConfig() {
				return config;
			}
			
			@Override
			public ByteToPacketConverter createToPacketConverter() {
				return toPack.get();
			}
			
			@Override
			public PacketToByteConverter createToByteConverter() {
				return toByte.get();
			}

			@Override
			public CoderThreadPool.Encoder getEncoderPool() {
				return new CoderThreadPool.Encoder(this, onEncoderError);
			}

			@Override
			public CoderThreadPool.Decoder getDecoderPool() {
				return new CoderThreadPool.Decoder(this, onDecoderError);
			}
		};
	}
}
