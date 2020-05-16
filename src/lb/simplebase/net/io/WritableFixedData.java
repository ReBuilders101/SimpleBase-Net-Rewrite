package lb.simplebase.io;

import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class WritableFixedData implements WritableByteData {

	public abstract byte[] getAsArray();
	
	public abstract byte[] internalArray();
	
	public abstract int getTotalLength();
	
	public abstract int getWrittenLength();
	
	public boolean isFull() {
		return getWrittenLength() == getTotalLength();
	}
	
	public static class WritableBufferData extends WritableFixedData {

		private final ByteBuffer data;
		
		public WritableBufferData(int capacity) {
			this.data = ByteBuffer.allocate(capacity);
		}
		
		@Override
		public void writeChar(char c) {
			data.putChar(c);
		}

		@Override
		public void writeShort(short s) {
			data.putShort(s);
		}

		@Override
		public void writeInt(int i) {
			data.putInt(i);
		}

		@Override
		public void writeLong(long l) {
			data.putLong(l);
		}

		@Override
		public void writeFloat(float f) {
			data.putFloat(f);
		}

		@Override
		public void writeDouble(double d) {
			data.putDouble(d);
		}

		@Override
		public void write(byte[] toWrite) {
			data.put(toWrite);
		}

		@Override
		public void writeByte(byte b) {
			data.put(b);
		}

		@Override
		public byte[] getAsArray() {
			return Arrays.copyOf(data.array(), data.capacity());
		}

		@Override
		public byte[] internalArray() {
			return data.array();
		}

		@Override
		public int getTotalLength() {
			return data.capacity();
		}

		@Override
		public int getWrittenLength() {
			return data.position();
		}
		
		public ByteBuffer getBuffer() {
			return data;
		}
		
	}
	
	public static class WritableArrayData extends WritableFixedData {

		private final byte[] data;
		private int writePointer;
		
		public WritableArrayData(int capacity) {
			this.data = new byte[capacity];
			this.writePointer = 0;
		}
		
		@Override
		public void writeByte(byte b) {
			data[writePointer++] = b; //Post-Increment
		}

		@Override
		public void write(byte[] toWrite) {
			System.arraycopy(toWrite, 0, data, writePointer, toWrite.length);
			writePointer += toWrite.length;
		}
		
		@Override
		public byte[] getAsArray() {
			return Arrays.copyOf(data, data.length);
		}

		@Override
		public byte[] internalArray() {
			return data;
		}

		@Override
		public int getTotalLength() {
			return data.length;
		}

		@Override
		public int getWrittenLength() {
			return writePointer;
		}
		
	}
	
}
