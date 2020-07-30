package dev.lb.simplebase.net.packet.format;

import java.nio.ByteBuffer;
import java.util.function.ObjIntConsumer;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.io.ByteDataHelper;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;

@Internal
class NetworkPacketFormat4Bytes<Connection> extends NetworkPacketFormat<Connection, PacketIDMappingProvider, Integer>{

	private final ObjIntConsumer<Connection> consumer;
	
	protected NetworkPacketFormat4Bytes(int uuid, ObjIntConsumer<Connection> consumer) {
		super(uuid);
		this.consumer = consumer;
	}

	@Override
	public int receiveMore(ByteBuffer currentBuffer, int length) {
		return 4 - length; //Need 4 bytes exactly
	}

	@Override
	public Integer decode(PacketIDMappingProvider context, ByteBuffer allBytes) {
		final int checkId = ByteDataHelper.cInt(allBytes);
		return Integer.valueOf(checkId);
	}

	@Override
	public void publish(Connection connection, Integer data) {
		consumer.accept(connection, data.intValue());
	}
}
