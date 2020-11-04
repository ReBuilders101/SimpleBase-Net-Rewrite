package dev.lb.simplebase.net.io;

import java.nio.ByteBuffer;

import dev.lb.simplebase.net.annotation.Internal;

/**
 * Implementation of {@link WritableByteData}, internally used to serialize packets
 */
@Internal
final class FixedNIOWritableData implements WritableNIOData {

	private final ByteBuffer data;
	
	protected FixedNIOWritableData(int size) {
		this.data = ByteBuffer.allocate(size);
	}
	
	@Override
	public void writeByte(byte b) {
		data.put(b);
	}
	
	@Override
	public ByteBuffer getBuffer() {
		data.flip();
		return data;
	}

}
