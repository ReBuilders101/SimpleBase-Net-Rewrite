package dev.lb.simplebase.net.io.read;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.io.ReadableByteData;

/**
 * Implementation of {@link ReadableByteData}, internally used to deserialize packets
 */
@Internal
public class ArrayReadableData implements ReadableByteData {

	private final byte[] data;
	private int currentIndex;
	
	public ArrayReadableData(byte[] data) {
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
