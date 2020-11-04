package dev.lb.simplebase.net.io;

import dev.lb.simplebase.net.annotation.Internal;

/**
 * Implementation of {@link WritableByteData}, internally used to serialize packets
 */
@Internal
final class FixedArrayWriteableData implements WritableArrayData {

	private final byte[] data;
	private int currentIndex;
	
	protected FixedArrayWriteableData(int size) {
		data = new byte[size];
		currentIndex = 0;
	}
	
	@Override
	public void writeByte(byte b) {
		data[currentIndex++] = b;
	}

	@Override
	public byte[] getArray() {
		if(currentIndex == data.length) { //filled exactly
			return data;
		} else {
			final byte[] result = new byte[currentIndex];
			System.arraycopy(data, 0, result, 0, currentIndex);
			return result;
		}
	}

}
