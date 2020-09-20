package dev.lb.simplebase.net.manager;

import java.util.function.Supplier;

import dev.lb.simplebase.net.config.CommonConfig;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.converter.ByteToPacketConverter;
import dev.lb.simplebase.net.packet.converter.PacketToByteConverter;

/**
 * Holds Properties of a {@link NetworkManagerCommon}, but is not always backet by an actual implmentation
 */
public interface NetworkManagerProperties {

	public CommonConfig<?> getConfig();
	public PacketIDMappingProvider getMappingContainer();
	
	public PacketToByteConverter createToByteConverter();
	public ByteToPacketConverter createToPacketConverter();
	
	public static NetworkManagerProperties of(CommonConfig<?> config, PacketIDMappingProvider provider,
			Supplier<PacketToByteConverter> toByte, Supplier<ByteToPacketConverter> toPacket) {
		return new NetworkManagerProperties() {
			
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
				return toPacket.get();
			}
			
			@Override
			public PacketToByteConverter createToByteConverter() {
				return toByte.get();
			}
		};
	}
}
