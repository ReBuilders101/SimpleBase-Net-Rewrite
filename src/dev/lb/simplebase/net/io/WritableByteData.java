package dev.lb.simplebase.net.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
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
	
	public default void writeTimeInstant(Instant instant) {
		writeTime(instant, DateTimeFormatter.ISO_INSTANT);
	}
	
	public default void writeTime(TemporalAccessor time, DateTimeFormatter formatter) {
		final String serialized = formatter.format(time);
		writeShortStringWithLength(serialized);
	}
	
	/**
	 * Writes a <code>short</code> value at the end of the current byte sequence, using two <code>byte</code> values. 
	 * @param s The <code>short</code> that should be written
	 */
	public default void writeShort(short s) {
		writeByte((byte) (s & 0xFF));
		writeByte((byte) ((s >>> 8) & 0xFF));
	}	
	
	/**
	 * Writes a <code>int</code> value at the end of the current byte sequence, using four <code>byte</code> values. 
	 * @param i The <code>int</code> that should be written
	 */
	public default void writeInt(int i) {
		writeByte((byte) (i & 0xFF));
		writeByte((byte) ((i >>> 8 ) & 0xFF));
		writeByte((byte) ((i >>> 16) & 0xFF));
		writeByte((byte) ((i >>> 24) & 0xFF));
	}	
	
	/**
	 * Writes a <code>long</code> value at the end of the current byte sequence, using eight <code>byte</code> values. 
	 * @param l The <code>long</code> that should be written
	 */
	public default void writeLong(long l) {
		writeByte((byte) (l & 0xFF));
		writeByte((byte) ((l >>> 8 ) & 0xFF));
		writeByte((byte) ((l >>> 16) & 0xFF));
		writeByte((byte) ((l >>> 24) & 0xFF));
		writeByte((byte) ((l >>> 32) & 0xFF));
		writeByte((byte) ((l >>> 40) & 0xFF));
		writeByte((byte) ((l >>> 48) & 0xFF));
		writeByte((byte) ((l >>> 56) & 0xFF));
	}
	
	/**
	 * Writes a <code>float</code> value at the end of the current byte sequence by encoding it as an <code>int</code>
	 * with the {@link Float#floatToRawIntBits(float)} method. 
	 * @param f The <code>float</code> that should be written
	 */
	public default void writeFloat(float f) {
		writeInt(Float.floatToRawIntBits(f));
	}
	
	/**
	 * Writes a <code>double</code> value at the end of the current byte sequence by encoding it as a <code>long</code>
	 * with the {@link Double#doubleToRawLongBits(double)} method. 
	 * @param d The <code>double</code> that should be written
	 */
	public default void writeDouble(double d) {
		writeLong(Double.doubleToRawLongBits(d));
	}
	
	/**
	 * Writes all bytes in the byte array at the end of the current byte sequence
	 * @param data The byte data that should be written
	 */
	public default void write(byte[] data) {
		for(byte b : data) {
			writeByte(b);
		}
	}
	
	/**
	 * Writes a {@link CharSequence} to the end of the current byte sequence, by converting it to a {@link String} using 
	 * {@link CharSequence#toString()} and the to a byte array using {@link String#getBytes()}.
	 * @param cs The {@link CharSequence} that should be written
	 * @see #writeStringWithLength(CharSequence)
	 */
	public default void writeString(CharSequence cs) {
		write(cs.toString().getBytes());
	}
	
	/**
	 * Writes a {@link CharSequence} to the end of the current byte sequence, by converting it to a {@link String} using 
	 * {@link CharSequence#toString()} and the to a byte array using {@link String#getBytes()}.<br>
	 * Additionally, the length of the {@link CharSequence} is written as an <code>int</code> in front of the {@link CharSequence}'s
	 * byte data.
	 * @param cs The {@link CharSequence} that should be written
	 * @see #writeString(CharSequence)
	 */
	public default void writeStringWithLength(CharSequence cs) {
		writeInt(cs.length());
		writeString(cs);
	}
	
	/**
	 * Writes a {@link CharSequence} to the end of the current byte sequence, by converting it to a {@link String} using 
	 * {@link CharSequence#toString()} and the to a byte array using {@link String#getBytes()}.<br>
	 * Additionally, the length of the {@link CharSequence} is written as an <code>byte</code> in front of the {@link CharSequence}'s
	 * byte data.<br>
	 * <b>If the CharSequence is longer than 255 chars, it will be cut at 255 chars.</b>
	 * @param cs The {@link CharSequence} that should be written
	 * @see #writeString(CharSequence)
	 */
	public default void writeShortStringWithLength(CharSequence cs) {
		writeByte((byte) cs.length());
		if(cs.length() > 255) {
			writeString(cs.subSequence(0, 255));
		} else {
			writeString(cs);
		}
	}
	
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
	 * An {@link OutputStream} that delegates all write() calls to this {@link WritableByteData}
	 * @return An {@link OutputStream} for this object
	 */
	public default OutputStream getOutStream() {
		return new OutputStream() {
			
			@Override
			public void write(int var1) throws IOException {
				writeByte( (byte) (var1 & 0xFF) );
			}
		}; 
	}
	
	/**
	 * Wraps a {@link WritableByteData} around a Java OutputStream.
	 * It only partially implements {@link ByteData}: The {@link #getAsArray()} and {@link #getAsArrayFast()} methods
	 * will return null except when the stream is a {@link ByteArrayOutputStream}.
	 * @param out The {@link OutputStream}
	 * @return The wrapped object
	 */
	public static WritableByteData wrap(final OutputStream out) {
		return new WritableByteData() {
			
			@Override
			public void writeByte(byte b) {
				try {
					out.write(b & 0xFF);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
	}
}
