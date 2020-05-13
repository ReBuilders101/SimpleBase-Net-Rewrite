package lb.simplebase.io;

import java.util.Arrays;

public class ReadableArrayData implements ReadableByteData {

	private final byte[] data;
	private int readPointer;
	
	public ReadableArrayData(byte[] data, boolean copy) {
		this.data = copy ? Arrays.copyOf(data, data.length) : data;
		this.readPointer = 0;
	}

	@Override
	public byte readByte() {
		return data[readPointer++]; //Post-Increment
	}

	@Override
	public byte[] read(int length) {
		final byte[] res = new byte[length];
		read(res);
		return res;
	}

	@Override
	public void read(byte[] toFill) {
		System.arraycopy(data, readPointer, toFill, 0, toFill.length);
		readPointer += toFill.length;
	}

	@Override
	public void skip(int amount) {
		readPointer += amount;
	}

	@Override
	public boolean canRead() {
		return readPointer < data.length;
	}

	@Override
	public byte[] getByteData() {
		return Arrays.copyOf(data, data.length);
	}

	@Override
	public int getByteLength() {
		return data.length;
	}

	@Override
	public int getRemainingLength() {
		return data.length - readPointer;
	}
	
	public byte[] internalArray() {
		return data;
	}
}
