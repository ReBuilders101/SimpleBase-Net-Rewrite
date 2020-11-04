package dev.lb.simplebase.net.io;

import java.nio.ByteBuffer;

import dev.lb.simplebase.net.annotation.Internal;

/**
 * Implementation of {@link ReadableByteData}, internally used to deserialize packets
 */
@Internal
final class NIOReadableData implements ReadableByteData {

	private final ByteBuffer data;
	
	protected NIOReadableData(ByteBuffer data) {
		this.data = data;
	}
	
	@Override
	public byte readByte() {
		return data.get();
	}

	@Override
	public void skip(int amount) {
		data.position(data.position() + amount);
	}

	@Override
	public boolean canRead() {
		return data.remaining() > 0;
	}

	@Override
	public int getRemainingLength() {
		return data.remaining();
	}

}
