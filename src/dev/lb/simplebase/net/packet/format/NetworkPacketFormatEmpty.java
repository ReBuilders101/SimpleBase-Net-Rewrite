package dev.lb.simplebase.net.packet.format;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import dev.lb.simplebase.net.annotation.Internal;

@Internal
class NetworkPacketFormatEmpty<Connection> extends NetworkPacketFormat<Connection, Object, Object> {

	protected NetworkPacketFormatEmpty(int uuid, String name, Consumer<Connection> consumer) {
		super(uuid, name);
		this.consumer = consumer;
	}

	private final Consumer<Connection> consumer;

	@Override
	public int receiveMore(ByteBuffer currentBuffer, int length) {
		return 0 - length; //O bytes
	}

	@Override
	protected Object decode(Object context, ByteBuffer allBytes) {
		return new Object(); //Can't be null because that discards the packet
	}

	@Override
	public void publish(Connection connection, Object data) {
		consumer.accept(connection);
	}

	@Override
	public ByteBuffer encode(Object context, Object data, int bufferSize) {
		return ByteBuffer.allocate(0);
	}
	
}
