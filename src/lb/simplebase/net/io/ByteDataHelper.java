package lb.simplebase.net.io;

import java.nio.ByteBuffer;

import dev.lb.simplebase.net.annotation.StaticType;

/**
 * Helper class to convert values to/from bytes
 */
@StaticType
public final class ByteDataHelper {

	private ByteDataHelper() {}
	
	/**
	 * Reads four bytes from the byte array and converts them into a single <code>int</code>.
	 * @param data The byte data, must have length 4
	 * @return The four bytes, as a <code>int</code>
	 */
	public static int cInt(byte[] data) {
		if(data.length != 4) throw new IllegalArgumentException("Array length must be exactly 4");
		//No enclosing typecast needed, result is already int
		return 	((( (int) data[3] ) & 0xFF) << 24) | //MSB has most left shift
				((( (int) data[2] ) & 0xFF) << 16) |
				((( (int) data[1] ) & 0xFF) << 8 ) |
				( ( (int) data[0] ) & 0xFF);		  //LSB not shifted
	}
	
	/**
	 * Reads four bytes from the byte buffer and converts them into a single <code>int</code>.
	 * @param data The byte data, will be read with relative gets
	 * @return The four bytes, as a <code>int</code>
	 */
	public static int cInt(ByteBuffer data) {
		final byte d0 = data.get();
		final byte d1 = data.get();
		final byte d2 = data.get();
		final byte d3 = data.get();
		//No enclosing typecast needed, result is already int
		return 	((( (int) d3 ) & 0xFF) << 24) | //MSB has most left shift
				((( (int) d2 ) & 0xFF) << 16) |
				((( (int) d1 ) & 0xFF) << 8 ) |
				( ( (int) d0 ) & 0xFF);		  //LSB not shifted
	}
	
	/**
	 * Writes a <code>int</code> value into the byte array, using four <code>byte</code> values. 
	 * @param i The <code>int</code> that should be written
	 * @param target The array to write into, must have length 4
	 */
	public static void cInt(int value, byte[] target, int offset) {
		if(target.length - offset < 4) throw new IllegalArgumentException("Array length must be exactly 4");
		
		target[0 + offset] = (byte) (value & 0xFF);
		target[1 + offset] = (byte) ((value >>> 8 ) & 0xFF);
		target[2 + offset] = (byte) ((value >>> 16) & 0xFF);
		target[3 + offset] = (byte) ((value >>> 24) & 0xFF);
	}
	
	/**
	 * Writes a <code>int</code> value into the byte buffer, using four <code>byte</code> values. 
	 * Writes in 4 <b>relative</b> byte puts
	 * @param i The <code>int</code> that should be written
	 * @param target The buffer to write into
	 */
	public static void cInt(int value, ByteBuffer target) {
		target.put((byte) (value & 0xFF));
		target.put((byte) ((value >>> 8 ) & 0xFF));
		target.put((byte) ((value >>> 16) & 0xFF));
		target.put((byte) ((value >>> 24) & 0xFF));
	}
	
	/**
	 * Writes a <code>int</code> value into the byte array, using four <code>byte</code> values. 
	 * @param i The <code>int</code> that should be written
	 * @return The byte array with length 4
	 */
	public static byte[] cInt(int value) {
		final byte[] data = new byte[4];
		cInt(value, data, 0);
		return data;
	}
}
