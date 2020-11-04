package dev.lb.simplebase.net.io;

/**
 * Interface for {@link WritableByteData} implementations that are backed by a byte array.
 */
public interface WritableArrayData extends WritableByteData {

	/**
	 * The current backing byte array used by write operations
	 * @return The backing array
	 */
	public byte[] getArray();
	
}
