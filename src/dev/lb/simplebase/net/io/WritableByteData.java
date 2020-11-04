package dev.lb.simplebase.net.io;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.UUID;

/**
 * The {@link WritableByteData} interface provides methods to write different primitives and basic objects to a stream of bytes.
 * <p>
 * The created byte stream is usually later parsed with {@link ReadableByteData}, which has corresponding read methods to every
 * write method defined in this interface.
 * </p><p>
 * The underlying byte array, buffer or stream can have a finite size. An attempt to write past the available capacity
 * will usually result in an exception of some type, although the exact behavior is not defined.
 * </p>
 */
public interface WritableByteData {

	/**
	 * Stores up to eight boolean values as bits of a single byte and writes that byte to the stream.
	 * <p>
	 * If more than eight values are passed into the varargs parameter, the first eight are written and the rest is ignored
	 * </p>
	 * @param values Up to eight boolean values
	 * @return {@code true} if all values were written, {@code false} if some had to be truncated
	 */
	public default boolean writeFlags(boolean...values) {
		byte b = 0;
		int max = values.length > 8 ? 8 : values.length; //sent amount of flags, length of array, but max 8
		for(int i = 0; i < max; i++) { //iterate over array
			if(values[i]) b |= (1 << i); //if flag is set, |= with the current power of 2
		}
		writeByte(b); //Write the byte
		return values.length <= 8; //if <= 8 -> all fit -> ok -> true 
	}
	
	/**
	 * Converts the boolean value to a byte and writes it to the stream.
	 * {@code true} will be encoded as 1, and {@code false} will be encoded as 0.
	 * @param value The boolean value to write to the stream
	 */
	public default void writeBoolean(boolean value) {
		writeByte(value ? (byte) 1 : (byte) 0); 
	}
	
	/**
	 * Writes a single byte to the stream.
	 * @param value The byte value to write to the stream
	 */
	public void writeByte(byte value);
	
	/**
	 * Writes a single unsigned byte to the stream. The integer is assumed to be in the range
	 * [0, 255]. If it is not, it will be masked with a {@code 0xFF} bitmask before converting
	 * it to a byte.
	 * @param value The unsigned byte value
	 */
	public default void writeByteU(int value) {
		writeByte((byte) (value & 0xFF));
	}
	
	/**
	 * Converts the char to two bytes and writes them to the stream.
	 * @param value The char to write to the stream
	 */
	public default void writeChar(char value) {
		writeByte((byte) (value & 0xFF));
		writeByte((byte) ((value >>> 8) & 0xFF));
	}
	
	/**
	 * Converts the {@link UUID} to 16 bytes by writing two 8-byte {@code long} values
	 * for the UUIDs most and least significant bits.
	 * @param value The {@link UUID} to write to the stream
	 */
	public default void writeUUID(UUID value) {
		writeLong(value.getMostSignificantBits());
		writeLong(value.getLeastSignificantBits());
	}
	
	/**
	 * Converts the {@link Instant} parameter to a {@link String} using the {@link DateTimeFormatter#ISO_INSTANT}
	 * and writes this string prfixed with its length (as defined in {@link #writeShortStringWithLength(CharSequence)}).
	 * <p>
	 * Use {@link #writeTime(TemporalAccessor, DateTimeFormatter)} for alternate time 
	 * representations and formats.
	 * </p>
	 * @param instant The {@link Instant} to write
	 */
	public default void writeTimeInstant(Instant instant) {
		writeTime(instant, DateTimeFormatter.ISO_INSTANT);
	}
	
	/**
	 * Converts the {@link TemporalAccessor} to a {@link String} using the supplied {@link DateTimeFormatter}
	 * and writes this string prfixed with its length (as defined in {@link #writeShortStringWithLength(CharSequence)}).
	 * @param time The {@link TemporalAccessor} containing the time value
	 * @param formatter The {@link DateTimeFormatter} used to encode the {@link TemporalAccessor}
	 */
	public default void writeTime(TemporalAccessor time, DateTimeFormatter formatter) {
		final String serialized = formatter.format(time);
		writeShortStringWithLength(serialized);
	}
	
	/**
	 * Converts the short to two bytes and writes them to the stream.
	 * @param value The short to write to the stream
	 */
	public default void writeShort(short value) {
		writeByte((byte) (value & 0xFF));
		writeByte((byte) ((value >>> 8) & 0xFF));
	}	
	
	/**
	 * Converts the int to four bytes and writes them to the stream.
	 * @param value The int to write to the stream
	 */
	public default void writeInt(int value) {
		writeByte((byte) (value & 0xFF));
		writeByte((byte) ((value >>> 8 ) & 0xFF));
		writeByte((byte) ((value >>> 16) & 0xFF));
		writeByte((byte) ((value >>> 24) & 0xFF));
	}	
	
	/**
	 * Converts the long to eight bytes and writes them to the stream.
	 * @param value The long to write to the stream
	 */
	public default void writeLong(long value) {
		writeByte((byte) (value & 0xFF));
		writeByte((byte) ((value >>> 8 ) & 0xFF));
		writeByte((byte) ((value >>> 16) & 0xFF));
		writeByte((byte) ((value >>> 24) & 0xFF));
		writeByte((byte) ((value >>> 32) & 0xFF));
		writeByte((byte) ((value >>> 40) & 0xFF));
		writeByte((byte) ((value >>> 48) & 0xFF));
		writeByte((byte) ((value >>> 56) & 0xFF));
	}
	
	/**
	 * Converts the float into four bytes using {@link Float#floatToRawIntBits(float)} and
	 * writes them to the stream.
	 * @param value The float to write to the stream
	 */
	public default void writeFloat(float value) {
		writeInt(Float.floatToRawIntBits(value));
	}
	
	/**
	 * Converts the double into eight bytes using {@link Double#doubleToRawLongBits(double)} and
	 * writes them to the stream.
	 * @param value The double to write to the stream
	 */
	public default void writeDouble(double value) {
		writeLong(Double.doubleToRawLongBits(value));
	}
	
	/**
	 * Writes all bytes in the byte array to the stream.
	 * No additional information (such as length of the array) is written.
	 * @param data The byte data to write to the stream
	 */
	public default void write(byte[] data) {
		for(byte b : data) {
			writeByte(b);
		}
	}
	
	/**
	 * Writes all bytes in the specified range to the stream.
	 * <br>
	 * The array offsets and indices are not validated before attempting to write.
	 * An exception may cause the write to fail in a partially completed state.
	 * @param data The byte data to (partially) write to the stream
	 * @param offset The index at which to start writing
	 * @param length The amount of bytes to write
	 */
	public default void write(byte[] data, int offset, int length) {
		for(int i = 0; i < length; i++) {
			writeByte(data[offset + 1]);
		}
	}
	
	/**
	 * Converts the {@link CharSequence} to bytes using {@link String#getBytes(java.nio.charset.Charset)} with
	 * the {@link StandardCharsets#UTF_8} charset and writes the bytes to the stream.
	 * @param value The {@link CharSequence} to write to the stream
	 * @see #writeStringWithLength(CharSequence)
	 */
	public default void writeString(CharSequence value) {
		write(value.toString().getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Writes the length of the string's byte representation as a prefix using and int value (4 bytes), followed
	 * by the string's byte representation as defined in {@link #writeString(CharSequence)}.
	 * @param value The {@link CharSequence} to write to the stream
	 */
	public default void writeStringWithLength(CharSequence value) {
		byte[] data = value.toString().getBytes(StandardCharsets.UTF_8);
		writeInt(data.length);
		write(data);
	}
	
	/**
	 * Writes the length of the string's byte representation as a prefix using and single byte value, followed
	 * by the string's byte representation as defined in {@link #writeString(CharSequence)}.
	 * <br>
	 * If the {@link CharSequence}'s byte representation is longer than 255 bytes, it will be truncated at that
	 * length. 
	 * @param value The {@link CharSequence} to write to the stream
	 */
	public default void writeShortStringWithLength(CharSequence value) {
		byte[] data = value.toString().getBytes(StandardCharsets.UTF_8);
		if(data.length > 255) {
			writeByteU(255);
			write(data, 0, 255);
		} else {
			writeByteU(data.length);
			write(data);
		}
	}
	
	/**
	 * Serializes the {@link Serializable} using Java's default serialization 
	 * method ({@link ObjectOutputStream}) and writes the serialized bytes to the stream.
	 * @param object The object to serialize
	 * @return {@code false} if an {@link IOException} occurred while writing the object, {@code true} otherwise
	 */
	public default boolean writeObject(Serializable object) {
		try(ObjectOutputStream oos = new ObjectOutputStream(getOutStream())) {
			oos.writeObject(object);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Creates an {@link OutputStream} that delegates all calls to {@link OutputStream#write(int)}
	 * to {@link #writeByteU(int)}.
	 * @return A new {@link OutputStream} delegating to this interface
	 */
	public default OutputStream getOutStream() {
		return new OutputStream() {
			@Override
			public void write(int value) throws IOException {
				writeByteU(value);
			}
		}; 
	}
	
	/**
	 * Wraps an {@link OutputStream} in a {@link WritableByteData} interface by redirecting all
	 * calls to the {@link WritableByteData#writeByte(byte)} method to the
	 * stream's {@link OutputStream#write(int)} method.<p>
	 * Any {@link IOException}s caused by the write operation on the stream will be
	 * wrapped in an {@link UncheckedIOException} and rethrown.</p>
	 * @param out The {@link OutputStream} to wrap
	 * @return A {@link WritableByteData} implementation wrapping the stream.
	 * @deprecated Outdated exception model
	 */
	@Deprecated
	public static WritableByteData wrap(final OutputStream out) {
		return new WritableByteData() {
			
			@Override
			public void writeByte(byte b) {
				try {
					out.write(b & 0xFF);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		};
	}
	
	/**
	 * Creates an empty {@link WritableByteData} instance that is backed by a byte array.
	 * @param initialCapacity The initial size of the array
	 * @param dynamic If {@code true}, the size of the backing array can dynamically increase
	 * @return A {@link WritableArrayData} instance
	 */
	public static WritableArrayData ofArray(int initialCapacity, boolean dynamic) {
		return dynamic ? new DynamicArrayWritableData(initialCapacity) : new FixedArrayWriteableData(initialCapacity);
	}
	
	/**
	 * Creates an empty {@link WritableByteData} instance that is backed by a {@link ByteBuffer}.
	 * @param initialCapacity The initial size of the buffer
	 * @param dynamic If {@code true}, the size of the backing buffer can dynamically increase
	 * @return A {@link WritableNIOData} instance
	 */
	public static WritableNIOData ofBuffer(int initialCapacity, boolean dynamic) {
		return dynamic ? new DynamicNIOWritableData(initialCapacity) : new FixedNIOWritableData(initialCapacity);
	}
}
