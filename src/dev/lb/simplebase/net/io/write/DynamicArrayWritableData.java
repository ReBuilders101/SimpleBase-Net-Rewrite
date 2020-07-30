package dev.lb.simplebase.net.io.write;

import dev.lb.simplebase.net.annotation.Internal;

@Internal
public class DynamicArrayWritableData implements WritableArrayData {

	private byte[] data;
	private int currentIndex;
	private final int sizeIncrement;
	
	public DynamicArrayWritableData(int initialSize) {
		data = new byte[initialSize];
		currentIndex = 0;
		sizeIncrement = initialSize;
	}
	
	public void ensureCapacity() {
		if(currentIndex == data.length) {
			byte[] newData = new byte[data.length + sizeIncrement];
			System.arraycopy(data, 0, newData, 0, data.length);
			//leave the index as-is, already points to the right location
			data = newData;
		}
	}
	
	@Override
	public void writeByte(byte b) {
		ensureCapacity();
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
