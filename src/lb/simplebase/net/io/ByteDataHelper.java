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
	 * @param data The byte data, will be read with absolute gets for indices 0-3
	 * @return The four bytes, as a <code>int</code>
	 */
	public static int cInt(ByteBuffer data) {
		//No enclosing typecast needed, result is already int
		return 	((( (int) data.get(3) ) & 0xFF) << 24) | //MSB has most left shift
				((( (int) data.get(2) ) & 0xFF) << 16) |
				((( (int) data.get(1) ) & 0xFF) << 8 ) |
				( ( (int) data.get(0) ) & 0xFF);		  //LSB not shifted
	}
	
	/**
	 * Writes a <code>int</code> value into the byte array, using four <code>byte</code> values. 
	 * @param i The <code>int</code> that should be written
	 * @param target The array to write into, must have length 4
	 */
	public static void cInt(int value, byte[] target) {
		if(target.length != 4) throw new IllegalArgumentException("Array length must be exactly 4");
		
		target[0] = (byte) (value & 0xFF);
		target[1] = (byte) ((value >>> 8 ) & 0xFF);
		target[2] = (byte) ((value >>> 16) & 0xFF);
		target[3] = (byte) ((value >>> 24) & 0xFF);
	}
	
	/**
	 * Writes a <code>int</code> value into the byte buffer, using four <code>byte</code> values. 
	 * Writes in absolute puts for indices 0-3
	 * @param i The <code>int</code> that should be written
	 * @param target The buffer to write into
	 */
	public static void cInt(int value, ByteBuffer target) {
		target.put(0, (byte) (value & 0xFF));
		target.put(1, (byte) ((value >>> 8 ) & 0xFF));
		target.put(2, (byte) ((value >>> 16) & 0xFF));
		target.put(3, (byte) ((value >>> 24) & 0xFF));
	}
	
	/**
	 * Writes a <code>int</code> value into the byte array, using four <code>byte</code> values. 
	 * @param i The <code>int</code> that should be written
	 * @return The byte array with length 4
	 */
	public static byte[] cInt(int value) {
		final byte[] data = new byte[4];
		cInt(value, data);
		return data;
	}
}
