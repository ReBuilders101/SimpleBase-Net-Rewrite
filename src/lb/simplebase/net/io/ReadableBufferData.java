package lb.simplebase.io;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ReadableBufferData implements ReadableByteData {

	private final ByteBuffer data;
	
	/**
	 * Buffer position will be reset to 0, the buffer will be read from 0 to the limit 
	 * @param data The buffer to use. Must not be native/direct
	 * @param copy If true, the buffer will be copied so that changes to the buffer object are not visible in this ReadableBufferData object. If false, the buffer will share data but have independent poition, mark, etc.
	 */
	public ReadableBufferData(ByteBuffer data, boolean copy) {
		this.data = copy ? deepCopy(data) : data.duplicate();
		data.rewind();
	}

	
	protected static ByteBuffer deepCopy(final ByteBuffer buffer) {
		assert !buffer.isDirect();
		final ByteBuffer buf2 = ByteBuffer.allocate(buffer.capacity());
		buf2.put(buffer);
		return buf2;
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
		return data.hasRemaining();
	}


	@Override
	public byte[] getByteData() {
		return Arrays.copyOf(data.array(), data.array().length);
	}


	@Override
	public int getByteLength() {
		return data.capacity();
	}


	@Override
	public int getRemainingLength() {
		return data.remaining();
	}


	@Override
	public char readChar() {
		return data.getChar();
	}


	@Override
	public short readShort() {
		return data.getShort();
	}


	@Override
	public int readInt() {
		return data.getInt();
	}


	@Override
	public long readLong() {
		return data.getLong();
	}


	@Override
	public float readFloat() {
		return data.getFloat();
	}


	@Override
	public double readDouble() {
		return data.getDouble();
	}


	@Override
	public byte[] read(int length) {
		byte[] ret = new byte[length];
		data.get(ret);
		return ret;
	}


	@Override
	public void read(byte[] toFill) {
		data.get(toFill);
	}
}
