package dev.lb.simplebase.net.packet.format;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.io.ByteDataHelper;
import dev.lb.simplebase.net.io.ReadableByteData;
import dev.lb.simplebase.net.io.WritableByteData;
import dev.lb.simplebase.net.io.WritableNIOData;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketIDMapping;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;

@Internal
class NetworkPacketFormat1Packet<Connection> extends NetworkPacketFormat<Connection, PacketIDMappingProvider, Packet> {

	private final BiConsumer<Connection, Packet> consumer;
	
	protected NetworkPacketFormat1Packet(int uuid, String name, BiConsumer<Connection, Packet> consumer) {
		super(uuid, name);
		this.consumer = consumer;
	}

	@Override
	public int receiveMore(ByteBuffer currentBuffer, int length) {
		if(length < 8) {
			//If 8 bytes are not yet read, read packet type and length (4 bytes each) completely
			return 8 - length;	
		} else if(length >= 8) {
			//If 8 bytes are available, read packet length
			currentBuffer.position(4); //Skip the packet type
			final int packetLength = ByteDataHelper.cInt(currentBuffer) + 8; //data + header
			return packetLength - length; //The not-yet read part. Will be negative if over-reading
		} else {
			return -1; //Invalid
		}
	}

	@Override
	public void publish(Connection connection, Packet data) {
		consumer.accept(connection, data);
	}

	@Override
	public Packet decode(PacketIDMappingProvider context, ByteBuffer allBytes) {
		final int type = ByteDataHelper.cInt(allBytes);
		allBytes.position(8); //Start of data
		//type and length are already skipped
		final ReadableByteData readableData = ReadableByteData.of(allBytes);
		final PacketIDMapping mapping = context.findMapping(type);

		if(mapping == null) {
			//Invalid mapping, maybe logging, cancel packet
			LOGGER.error("Unknown packet id: " + type + " - no matching PacketIdMapping in manager"); 
			return null;
		}

		//make the packet and fill in data
		final Packet packet = mapping.createNewInstance();
		packet.readData(readableData);

		//return the packet
		return packet;
	}

	@Override
	public ByteBuffer encode(PacketIDMappingProvider context, Packet data, int bufferSize) {
		final PacketIDMapping mapping = context.findMapping(data.getClass());
		if(mapping == null) {
			LOGGER.error("Unknown packet type: " + data.getClass() + " - no matching PacketIdMapping in manager"); 
			return null;
		}
		final int packetId = mapping.getPacketID();
		final int expectedSize = data.getByteSize();
		final WritableNIOData writableData = WritableByteData.ofBuffer(expectedSize < 0 ? bufferSize : expectedSize, expectedSize < 0);
		
		data.writeData(writableData);
		final ByteBuffer buffer = writableData.getBuffer(); //Buffer is ready for read
		final int packetDataLength = buffer.remaining();
		
		final ByteBuffer completeData = ByteBuffer.allocate(packetDataLength + 8);
		ByteDataHelper.cInt(packetId, completeData);
		ByteDataHelper.cInt(packetDataLength, completeData);
		completeData.put(buffer);
		
		completeData.flip(); //prepare for read
		return completeData;
	}

	@Override
	public boolean supportsCompression() {
		return true;
	}
}