package lb.simplebase.net.io.write;

import dev.lb.simplebase.net.annotation.Internal;
import lb.simplebase.net.io.WritableByteData;

/**
 * Implementation of {@link WritableByteData}, internally used to serialize packets
 */
@Internal
public class FixedArrayWriteableData implements WritableArrayData {

	private final byte[] data;
	private int currentIndex;
	
	public FixedArrayWriteableData(int size) {
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
