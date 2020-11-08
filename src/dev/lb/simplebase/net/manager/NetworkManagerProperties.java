package dev.lb.simplebase.net.manager;

import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;

import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.connection.CoderThreadPool;
import dev.lb.simplebase.net.event.EventAccessor;
import dev.lb.simplebase.net.event.EventDispatcher;
import dev.lb.simplebase.net.packet.PacketIDMapping;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.converter.ByteToPacketConverter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;
import dev.lb.simplebase.net.util.Lazy;

/**
 * Holds Properties of a {@link NetworkManagerCommon}, but is not always backed by an actual implmentation
 */
public interface NetworkManagerProperties {

	/**
	 * The configuration object used by this network manager. The returned object will
	 * be locked ({@link CommonConfig#isLocked()}) and effectively immutable.
	 * @return The configuation object for this manager
	 */
	public CommonConfig getConfig();
	
	/**
	 * The container for {@link PacketIDMapping}s that are used to convert the packets sent form this manager to bytes.
	 * @return A {@link PacketIDMappingProvider} that holds the mappings for this manager
	 */
	public PacketIDMappingProvider getMappingContainer();
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * A {@link PacketToByteConverter} that is configured with the configs of this manager
	 * </p>
	 * @return A {@link PacketToByteConverter} used by this manager's connections
	 */
	public PacketToByteConverter createToByteConverter();
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * A {@link ByteToPacketConverter} that is configured with the configs of this manager
	 * </p>
	 * @return A {@link ByteToPacketConverter} used by this manager's connections
	 */
	public ByteToPacketConverter createToPacketConverter();
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * A {@link CoderThreadPool} that is used for encoding packets
	 * </p>
	 * @return A {@link CoderThreadPool} that is used for encoding packets
	 */
	public CoderThreadPool.Encoder getEncoderPool();
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and should not be called directly.
	 * </p><hr><p>
	 * A {@link CoderThreadPool} that is used for decoding packets
	 * </p>
	 * @return A {@link CoderThreadPool} that is used for decoding packets
	 */
	public CoderThreadPool.Decoder getDecoderPool();
	
	/**
	 * Creates an implementation of {@link NetworkManagerProperties} that is not backed by an actual manager
	 * @param config The {@link CommonConfig} to use
	 * @param provider The {@link PacketIDMappingProvider} to use
	 * @param onEncoderError An event handler for a encoder error (see {@link EventDispatcher#p1Dispatcher(EventAccessor, Function)}
	 * @param onDecoderError An event handler for a decoder error (see {@link EventDispatcher#p1Dispatcher(EventAccessor, Function)}
	 * @return A new {@link NetworkManagerProperties} with the desired properties
	 */
	public static NetworkManagerProperties of(CommonConfig config, PacketIDMappingProvider provider,
			Predicate<RejectedExecutionException> onEncoderError, Predicate<RejectedExecutionException> onDecoderError) {
		return new NetworkManagerProperties() {
			private final Lazy<PacketToByteConverter> toByte = Lazy.of(() -> new PacketToByteConverter(this));
			private final Lazy<ByteToPacketConverter> toPack = Lazy.of(() -> new ByteToPacketConverter(this));			
			@Override
			public PacketIDMappingProvider getMappingContainer() {
				return provider;
			}
			
			@Override
			public CommonConfig getConfig() {
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
				return NetworkManagerCommon.stackHackE(config, onEncoderError);
			}

			@Override
			public CoderThreadPool.Decoder getDecoderPool() {
				return NetworkManagerCommon.stackHackD(config, onDecoderError);
			}
		};
	}
}
