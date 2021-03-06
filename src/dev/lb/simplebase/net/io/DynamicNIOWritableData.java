package dev.lb.simplebase.net.io;

import java.nio.ByteBuffer;
import dev.lb.simplebase.net.annotation.Internal;

/**
 * Implementation of {@link WritableByteData}, internally used to serialize packets
 */
@Internal
final class DynamicNIOWritableData implements WritableNIOData {

	private ByteBuffer data;
	private final int sizeIncrement;
	
	protected DynamicNIOWritableData(int initialSize) {
		this.data = ByteBuffer.allocate(initialSize);
		this.sizeIncrement = initialSize;
	}
	
	private void ensureCapacity() {
		//No bytes left to write
		if(data.remaining() == 0) {
			final ByteBuffer newBuffer = ByteBuffer.allocate(data.capacity() + sizeIncrement);
			data.rewind(); //read the buffer from the start
			newBuffer.put(data); //copy over the old data
			//Replace the instance
			data = newBuffer;
		}
	}
	
	@Override
	public void writeByte(byte b) {
		ensureCapacity();
		data.put(b);
	}
	
	@Override
	public ByteBuffer getBuffer() {
		data.flip();
		return data;
	}
	
}
