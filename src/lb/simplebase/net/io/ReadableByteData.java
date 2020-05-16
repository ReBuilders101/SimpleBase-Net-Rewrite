package lb.simplebase.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Optional;

/**
 * This interface provides additional methods for reading primitives and strings directly. All methods
 * depend on the {@link #readByte()} method, which is defined by the implementation. by default,
 * data is encoded as Little Endian. Methods may be overridden by implementing classes, as long as compatibility is not broken.
 * <br>This interface is fully compatible to the data written by the {@link WritableByteData} interface and all valid implementations.
 */
public interface ReadableByteData {

	/**
	 * Reads 8 boolean values, which are stored in the 8 bits of one byte.
	 * The returned array always has a size of 8.
	 * @return A boolean array containing 8 boolean values
	 */
	public default boolean[] readFlags() {
		boolean[] ret = new boolean[8]; //Create array to fill
		byte b = readByte(); //read the byte that acts as 8 booleans
		for(int i = 0; i < 8; i++) { //Iterate 8 times
			ret[i] = (b & (1 << i)) != 0; //Test if bit at position i is not 0, fill array ( (1 << i) is a mask for powers of 2 from 2^0 (1) to 2^7 (128) )
		}
		return ret;
	}
	
	/**
	 * Reads a single boolean value from the next byte. The result will be <code>false</code>
	 * when the byte is <code>0</code>, and <code>true</code> when it is any other number
	 * (only <code>1</code> should be possible if this method is used correctly).
	 * @return The next byte as a <code>boolean</code> value
	 */
	public default boolean readBoolean() {
		return readByte() != 0;
	}
	
	/**
	 * Reads one byte from the byte sequence.
	 * @return The next <code>byte</code>
	 */
	public byte readByte();
	
	//LEAST SIGNIFICANT FIRST!!!!!
	//CAST BYTES TO TYPE BEFORE SHIFTING!!!!!
	
	/**
	 * Reads two bytes from the byte sequence and converts them into a single <code>char</code>.
	 * @return The next two bytes, as a <code>char</code>
	 */
	public default char readChar() {
		byte b0 = readByte(); //least significant
		byte b1 = readByte(); //most significant
		return (char) ( //Convert result to char
				((( (char) b1) & 0xFF ) << 8) | //MSB shifted left
				((  (char) b0) & 0xFF ) //LSB stays right
				);
	}
	
	/**
	 * Reads two bytes from the byte sequence and converts them into a single <code>short</code>.
	 * @return The next two bytes, as a <code>short</code>
	 */
	public default short readShort() {
		byte b0 = readByte(); //least significant
		byte b1 = readByte(); //most significant
		return (short) ( //Convert result to short
				((( (short) b1) & 0xFF ) << 8) | //MSB shifted left
				((  (short) b0) & 0xFF )); //LSB stays rigth
	}
	
	/**
	 * Reads four bytes from the byte sequence and converts them into a single <code>int</code>.
	 * @return The next four bytes, as a <code>int</code>
	 */
	public default int readInt() {
		byte b0 = readByte(); //LSB
		byte b1 = readByte();
		byte b2 = readByte();
		byte b3 = readByte(); //MSB
		//No enclosing typecast needed, result is already int
		return 	((( (int) b3 ) & 0xFF) << 24) | //MSB has most left shift
				((( (int) b2 ) & 0xFF) << 16) |
				((( (int) b1 ) & 0xFF) << 8 ) |
				( ( (int) b0 ) & 0xFF);		  //LSB not shifted
	}
	
	/**
	 * Reads eight bytes from the byte sequence and converts them into a single <code>long</code>.
	 * @return The next eight bytes, as a <code>long</code>
	 */
	public default long readLong() {
		//So many bytes, read through loop
		byte[] bytes = read(8); //Read all 8 bytes, bytes[0] will be LSB
		long result = 0;
		for(int i = 0; i < 8; i++) {
			//Offset starts with 0 and array starts with LSB -> LSB is not shifted -> ok
			result |= ( ( ((long)bytes[i]) & 0xFF ) << (i * 8)); //you can use &= , |= or += because numbers don't 'overlap'
		}
		return result;
	}
	
	/**
	 * Reads four bytes as an <code>int</code> and then converts them to <code>float</code> using {@link Float#intBitsToFloat(int)}.
	 * @return The next four bytes, as a <code>float</code>
	 */
	public default float readFloat() {
		return Float.intBitsToFloat(readInt());
	}
	
	/**
	 * Reads eight bytes as a <code>long</code> and then converts them to <code>double</code> using {@link Double#longBitsToDouble(long)}.
	 * @return The next eight bytes, as a <code>double</code>
	 */
	public default double readDouble() {
		return Double.longBitsToDouble(readLong());
	}
	
	/**
	 * Skips <i>amount</i> bytes. The next call to {@link #readByte()} (or any other method reading data, which depend on this method)
	 * will be offset by <i>amount</i> from the position it was on before the call.
	 * @param amount The amount of bytes that should be skipped 
	 */
	public void skip(int amount);
	
	/**
	 * Creates a new byte array and the fills it with <i>length</i> bytes from the byte sequence.<br>
	 * The array will always have a length of <i>length</i>.
	 * @param length The length of the byte array
	 * @return The created byte array
	 */
	public default byte[] read(int length) {
//		assure(length); //no assure, since this is done in #read(byte[])
		byte[] ret = new byte[length];
		read(ret);
		return ret;
	}
	
	/**
	 * Reads <i>toFill.length</i> bytes from the byte sequence and fills the array with that data.
	 * All values contained in the byte array will be replaced.
	 * @param toFill The array that will be filled with values
	 */
	public default void read(byte[] toFill) {
		for(int i = 0; i < toFill.length; i++) {
			toFill[i] = readByte();
		}
	}
	
	/**
	 * Reads <i>length</i> bytes and then converts them to a String using {@link String#String(byte[])}
	 * @param length The length of the {@link String}
	 * @return The created {@link String}
	 * @see #readStringWithLength()
	 */
	public default String readString(int length) {
		byte[] stringData = read(length);
		return new String(stringData);
	}
	
	/**
	 * Reads an <code>int</code> representing the length of the {@link String}, and then reads
	 * this amount of bytes as the string data. The new {@link String} is created using {@link String#String(byte[])}
	 * @return The created {@link String}
	 * @see #readString(int)
	 */
	public default String readStringWithLength() {
		return readString(readInt());
	}
	
	/**
	 * Reads an <code>byte</code> representing the length of the {@link String}, and then reads
	 * this amount of bytes as the string data. The new {@link String} is created using {@link String#String(byte[])}
	 * @return The created {@link String}
	 * @see #readString(int)
	 */
	public default String readShortStringWithLength() {
		return readString(readByte() & 0xFF);
	}
	
	public default Object readObject() {
		try (ObjectInputStream ois = new ObjectInputStream(getInStream())) {
			return ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace(); 
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public default <T> T readObject(Class<T> clazz) {
		return (T) readObject();
	}
	
	@SuppressWarnings("unchecked")
	public default <T> Optional<T> readObjectOptional(Class<T> clazz) {
		try (ObjectInputStream ois = new ObjectInputStream(getInStream())) {
			return Optional.of((T) ois.readObject());
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace(); 
			return Optional.empty();
		}
	}
	
	/**
	 * Returns true only if another byte can be read from the data source.
	 * @return Whether another byte can be read
	 */
	public boolean canRead();
	
	public byte[] getByteData();
	
	public int getByteLength();
	
	public int getRemainingLength();
	
	public default InputStream getInStream() {
		return new InputStream() {
			
			@Override
			public int read() throws IOException {
				if(!canRead()) return -1;
				return readByte() & 0xFF;
			}
		};
	}
	
}
