package dev.lb.simplebase.net.packet.format;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;

@Internal
class NetworkPacketFormatEmpty<Connection> extends NetworkPacketFormat<Connection, PacketIDMappingProvider, Object> {

	protected NetworkPacketFormatEmpty(int uuid, Consumer<Connection> consumer) {
		super(uuid);
		this.consumer = consumer;
	}

	private final Consumer<Connection> consumer;

	@Override
	public int receiveMore(ByteBuffer currentBuffer, int length) {
		return 0 - length; //O bytes
	}

	@Override
	protected Object decode(PacketIDMappingProvider context, ByteBuffer allBytes) {
		return new Object(); //Can't be null because that discards the packet
	}

	@Override
	public void publish(Connection connection, Object data) {
		consumer.accept(connection);
	}
	
}
