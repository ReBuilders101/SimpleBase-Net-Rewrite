package dev.lb.simplebase.net.packet.format;

import java.nio.ByteBuffer;
import java.util.function.ObjIntConsumer;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.io.ByteDataHelper;

@Internal
class NetworkPacketFormat4Bytes<Connection> extends NetworkPacketFormat<Connection, Object, Integer>{

	private final ObjIntConsumer<Connection> consumer;
	
	protected NetworkPacketFormat4Bytes(int uuid, String name, ObjIntConsumer<Connection> consumer) {
		super(uuid, name);
		this.consumer = consumer;
	}

	@Override
	public int receiveMore(ByteBuffer currentBuffer, int length) {
		return 4 - length; //Need 4 bytes exactly
	}

	@Override
	public Integer decode(Object context, ByteBuffer allBytes) {
		final int checkId = ByteDataHelper.cInt(allBytes);
		return Integer.valueOf(checkId);
	}

	@Override
	public void publish(Connection connection, Integer data) {
		consumer.accept(connection, data.intValue());
	}

	@Override
	public ByteBuffer encode(Object context, Integer data) {
		final ByteBuffer buffer = ByteBuffer.allocate(4);
		ByteDataHelper.cInt(data.intValue(), buffer);
		buffer.flip();
		return buffer;
	}
}
