package dev.lb.simplebase.net.io;

import dev.lb.simplebase.net.annotation.Internal;

/**
 * Implementation of {@link ReadableByteData}, internally used to deserialize packets
 */
@Internal
final class ArrayReadableData implements ReadableByteData {

	private final byte[] data;
	private int currentIndex;
	
	protected ArrayReadableData(byte[] data) {
		this.data = data;
		this.currentIndex = 0;
	}
	
	@Override
	public byte readByte() {
		return data[currentIndex++];
	}

	@Override
	public void skip(int amount) {
		currentIndex += amount;
	}

	@Override
	public boolean canRead() {
		return currentIndex < data.length;
	}

	@Override
	public int getRemainingLength() {
		return data.length - currentIndex;
	}

}
