package dev.lb.simplebase.net.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import java.util.UUID;

/**
 * The {@link ReadableByteData} interface provides methods to read different primitives and basic objects from a stream of bytes.
 * <p>
 * The byte stream is usually created with {@link WritableByteData}, which has a corresponding write method for every read method defined
 * in this interface.
 * </p><p>
 * The undelying byte array, buffer or stream can have a finite size. An attempt to read past the end of the available
 * data will usually result in an exception of some type, although the exact behavior is not defined.
 * </p>
 */
public interface ReadableByteData {

	/**
	 * Reads a single byte from the stream and interprets the byte as a set of 8 {@code boolean} flags,
	 * which are stored in an array.
	 * @return A {@code boolean} array of size 8
	 */
	public default boolean[] readFlags() {
		final byte b = readByte(); //read the byte that acts as 8 booleans
		final boolean[] ret = new boolean[8]; //Create array to fill
		for(int i = 0; i < 8; i++) { //Iterate 8 times
			ret[i] = (b & (1 << i)) != 0; //Test if bit at position i is not 0, fill array ( (1 << i) is a mask for powers of 2 from 2^0 (1) to 2^7 (128) )
		}
		return ret;
	}
	
	/**
	 * Reads a single byte from the stream and interprets the byte as one {@code boolean} value.
	 * A value of 0 will be converted to {@code false}, and every other value will
	 * be converted to {@code true}.
	 * @return The next byte of the stream as a {@code boolean}
	 */
	public default boolean readBoolean() {
		return readByte() != 0;
	}
	
	/**
	 * Reads a single byte from the stream.
	 * @return The next byte in the stream
	 */
	public byte readByte();
	
	/**
	 * Reads a single byte from the stream and returns an {@code int} containing 
	 * the byte's unsigned value (Range [0,255]).
	 * @return The next byte as an unsigned value
	 */
	public default int readByteU() {
		return ((int) readByte()) & 0xFF;
	}
	
	//LEAST SIGNIFICANT FIRST!!!!!
	//CAST BYTES TO TYPE BEFORE SHIFTING!!!!!
	
	/**
	 * Reads two bytes from the stream and converts them into a single {@code char} value.
	 * @return The next two bytes as a {@code char}
	 */
	public default char readChar() {
		final byte b0 = readByte(); //least significant
		final byte b1 = readByte(); //most significant
		return (char) ( //Convert result to char
				((( (char) b1) & 0xFF ) << 8) | //MSB shifted left
				((  (char) b0) & 0xFF ) //LSB stays right
				);
	}
	
	/**
	 * Reads 16 bytes from the stream and converts them to an instance of {@link UUID}.
	 * The UUID is encoded as two 8-byte {@code long} values: The UUID's most and least
	 * significant bits.
	 * @return The next 16 bytes as a {@link UUID}
	 */
	public default UUID readUUID() {
		final long msb = readLong();
		final long lsb = readLong();
		return new UUID(msb, lsb);
	}
	
	/**
	 * Reads a variable-length short string (as defined in {@link #readShortStringWithLength()})
	 * and converts that string to an {@link Instant} using the {@link DateTimeFormatter#ISO_INSTANT}
	 * formatter.
	 * <p>
	 * Use {@link #readTime(DateTimeFormatter)} for alternate time representations and formats.
	 * </p>
	 * @return A variable-length string converted to an {@link Instant}
	 */
	public default Instant readTimeInstant() {
		return Instant.from(readTime(DateTimeFormatter.ISO_INSTANT));
	}
	
	/**
	 * Reads a variable length short string (as defined in {@link #readShortStringWithLength()})
	 * and converts that string to a {@link TemporalAccessor} using the supplied {@link DateTimeFormatter}.
	 * <p>
	 * Many implementations of {@code TemporalAccessor} feature a static method to convert a {@code TemporalAccessor}
	 * to that type (such as {@link Instant#from(TemporalAccessor)}).
	 * </p>
	 * @param formatter The {@link DateTimeFormatter} used to decode the string
	 * @return A variable-length string converted to a {@link TemporalAccessor}
	 */
	public default TemporalAccessor readTime(DateTimeFormatter formatter) {
		final String serialized = readShortStringWithLength();
		return formatter.parse(serialized);
	}
	
	/**
	 * Reads two bytes from the stream and converts them into a single {@code short} value.
	 * @return The next two bytes as a {@code short}
	 */
	public default short readShort() {
		final byte b0 = readByte(); //least significant
		final byte b1 = readByte(); //most significant
		return (short) ( //Convert result to short
				((( (short) b1) & 0xFF ) << 8) | //MSB shifted left
				((  (short) b0) & 0xFF )); //LSB stays rigth
	}
	
	/**
	 * Reads four bytes from the stream and converts them into a single {@code int} value.
	 * @return The next four bytes as a {@code int}
	 */
	public default int readInt() {
		final byte b0 = readByte(); //LSB
		final byte b1 = readByte();
		final byte b2 = readByte();
		final byte b3 = readByte(); //MSB
		//No enclosing typecast needed, result is already int
		return 	((( (int) b3 ) & 0xFF) << 24) | //MSB has most left shift
				((( (int) b2 ) & 0xFF) << 16) |
				((( (int) b1 ) & 0xFF) << 8 ) |
				( ( (int) b0 ) & 0xFF);		  //LSB not shifted
	}
	
	/**
	 * Reads eight bytes from the stream and converts them into a single {@code long} value.
	 * @return The next eight bytes as a {@code long}
	 */
	public default long readLong() {
		//So many bytes, read through loop
		final byte[] bytes = read(8); //Read all 8 bytes, bytes[0] will be LSB
		long result = 0;
		for(int i = 0; i < 8; i++) {
			//Offset starts with 0 and array starts with LSB -> LSB is not shifted -> ok
			result |= ( ( ((long)bytes[i]) & 0xFF ) << (i * 8)); //you can use |= or += because numbers don't 'overlap'
		}
		return result;
	}
	
	/**
	 * Reads four bytes from the stream and converts them into a single {@code float} value using {@link Float#intBitsToFloat(int)}.
	 * @return The next four bytes as a {@code float}
	 */
	public default float readFloat() {
		return Float.intBitsToFloat(readInt());
	}
	
	/**
	 * Reads eight bytes from the stream and converts them into a single {@code double} value using {@link Double#longBitsToDouble(long)}.
	 * @return The next eight bytes as a {@code double}
	 */
	public default double readDouble() {
		return Double.longBitsToDouble(readLong());
	}
	
	/**
	 * Skips reading {@code amount} bytes from the stream.
	 * @param amount The amount of bytes to skip
	 */
	public void skip(int amount);
	
	
	/**
	 * Creates a new byte array of the requested length and fills it with the next available bytes.
	 * The created array is returned
	 * @param length The length of the array, and the amount of bytes to read
	 * @return The created array filled with data
	 */
	public default byte[] read(int length) {
		final byte[] ret = new byte[length];
		read(ret);
		return ret;
	}
	
	/**
	 * Reads {@code array.length} bytes from the stream and fills that array with these bytes
	 * @param array The array to fill with data
	 */
	public default void read(byte[] array) {
		read(array, 0, array.length);
	}
	
	/**
	 * Reads {@code length} bytes from the stream and fils the array with these bytes,
	 * starting at index {@code offset} (and ending at {@code offset + length}).
	 * @param array The array to (partially) fill with data
	 * @param offset The offset index at which to start filling data
	 * @param length The amount of bytes to write to the array
	 */
	public default void read(byte[] array, int offset, int length) {
		for(int i = 0; i < length; i++) {
			array[i + offset] = readByte();
		}
	}
	
	/**
	 * Reads a {@code length} bytes from the stream and converts them to a {@link String}
	 * using the {@link StandardCharsets#UTF_8} charset.
	 * @param length The length of the string, in bytes (not codepoints or characters)
	 * @return The decoded {@link String}
	 */
	public default String readString(int length) {
		byte[] stringData = read(length);
		return new String(stringData, StandardCharsets.UTF_8);
	}
	
	/**
	 * This method first reads a 4-byte integer. This is interpreted as the byte length of the
	 * encoded {@link String}. Then the string is decoded using the {@link StandardCharsets#UTF_8}
	 * charset (as in {@link #readString(int)}).
	 * @return The decoded {@link String}
	 */
	public default String readStringWithLength() {
		return readString(readInt());
	}
	
	/**
	 * This method first reads a single byte. This is interpreted as the byte length of the
	 * encoded {@link String}. This limits the length of a string read with this method to 255 bytes.
	 * Then the string is decoded using the {@link StandardCharsets#UTF_8} charset.
	 * charset (as in {@link #readString(int)}).
	 * @return The decoded {@link String}
	 */
	public default String readShortStringWithLength() {
		return readString(readByteU());
	}
	
	/**
	 * Reads a {@link Serializable} object using {@link ObjectInputStream#readObject()}.
	 * If any exception is thrown while reading the object, this method returns {@code null}
	 * after logging the error. 
	 * @return The read object, or {@code null} in case of error
	 */
	public default Object readObject() {
		try (ObjectInputStream ois = new ObjectInputStream(getInStream())) {
			return ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace(); 
			return null;
		}
	}
	
	/**
	 * Reads a {@link Serializable} object using {@link ObjectInputStream#readObject()}.
	 * If any exception is thrown while reading the object, this method returns {@code null}
	 * after logging the error.
	 * <p>
	 * The read object will be casted to the requested type. 
	 * </p>
	 * @param <T> The type to convert the object to
	 * @param clazz The class of the type to convert the object to
	 * @return The read object, or {@code null} in case of error
	 * @throws ClassCastException When the type of the read object and the requested type do not match
	 */
	@SuppressWarnings("unchecked")
	public default <T extends Serializable> T readObject(Class<T> clazz) {
		return (T) readObject();
	}
	
	/**
	 * Reads a {@link Serializable} object using {@link ObjectInputStream#readObject()}.
	 * If any exception is thrown while reading the object, this method returns an empty {@link Optional}
	 * after logging the error.
	 * <p>
	 * The read object will be casted to the requested type. 
	 * </p>
	 * @param <T> The type to convert the object to
	 * @param clazz The class of the type to convert the object to
	 * @return An {@link Optional} holding the read object, or an empty one in case of error
	 * @throws ClassCastException When the type of the read object and the requested type do not match
	 */
	@SuppressWarnings("unchecked")
	public default <T extends Serializable> Optional<T> readObjectOptional(Class<T> clazz) {
		try (ObjectInputStream ois = new ObjectInputStream(getInStream())) {
			return Optional.of((T) ois.readObject());
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace(); 
			return Optional.empty();
		}
	}
	
	/**
	 * Whether another byte can be read from the underlying data structure without causing an
	 * exception or reading an invalid value
	 * @return {@code true} if a byte can be read from the stream without throwing an exception
	 */
	public boolean canRead();
	
	/**
	 * The amount of bytes that are currently left in this stream.
	 * The number of remaining bytes can change even if no bytes are read; it is possible
	 * that the undelying data structure is expanded while it is being read. This behavior is
	 * implementation-dependent.
	 * @return The amount of bytes currently left in the stream
	 */
	public int getRemainingLength();
	
	/**
	 * Provides an {@link InputStream} that is a view of this {@link ReadableByteData}.
	 * The {@code InputStream}'s {@code read()} method is implemented as follows:
	 * <ul>
	 * <li>If {@link #canRead()} returns {@code true}, the method returns the next byte
	 * as an unsigned value (As in {@link #readByteU()}).</li>
	 * <li>If {@link #canRead()} returns {@code false}, the method returns -1
	 * (as defined in {@link InputStream#read()}).</li>
	 * </ul>
	 * An {@link IOException} is never thrown.
	 * @return An {@link InputStream} view of this {@link ReadableByteData}
	 */
	public default InputStream getInStream() {
		return new InputStream() {
			
			@Override
			public int read() throws IOException {
				if(!canRead()) return -1;
				return readByte() & 0xFF;
			}
		};
	}
	
	/**
	 * Creates a {@link ReadableByteData} instance that is a view of a byte array.
	 * Every created instance maintains its own read pointer.
	 * @param arraydata The array holding the data
	 * @return A view of the array data
	 */
	public static ReadableByteData of(byte[] arraydata) {
		return new ArrayReadableData(arraydata);
	}
	
	/**
	 * Creates a {@link ReadableByteData} instance that is a view of a byte buffer.
	 * The {@link ByteBuffer}'s position property is used as the read pointer, and
	 * if more than one instance is created for the same buffer, they will share a read pointer.
	 * @param bufferdata The {@link ByteBuffer} holding the data
	 * @return A view of the buffer data
	 */
	public static ReadableByteData of(ByteBuffer bufferdata) {
		return new NIOReadableData(bufferdata);
	}
}
