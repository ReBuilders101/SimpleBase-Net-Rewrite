package dev.lb.simplebase.net.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * This interface provides additional methods for writing primitives and strings. All methods
 * depend on the {@link #writeByte(byte)} method, which is defined by the implementation. by default,
 * data is encoded as Little Endian. Methods may be overridden by implementing classes, as long as compatibility is not broken.
 * <br>This interface is fully compatible to the data that should be read by the {@link ReadableByteData} interface and all valid implementations.
 */
public interface WritableByteData {

	/**
	 * Writes a maximum of 8 boolean values, encoded as a byte, to the end of the byte sequence.
	 * If the array has more than 8 elements, the additional elements are ignored, and <code>false</code> is returned. 
	 * @param flags The boolean values that should be written
	 * @return True if all values could be written
	 */
	public default boolean writeFlags(boolean...flags) {
		byte b = 0;
		int max = flags.length > 8 ? 8 : flags.length; //sent amount of flags, length of array, but max 8
		for(int i = 0; i < max; i++) { //iterate over array
			if(flags[i]) b |= (1 << i); //if flag is set, |= with the current power of 2
		}
		writeByte(b); //Write the byte
		return flags.length <= 8; //if <= 8 -> all fit -> ok -> true 
	}
	
	/**
	 * Writes a single boolean value, represented by a <code>0</code> for <code>false</code>
	 * and a <code>1</code> for <code>true</code>.<br>
	 * This method uses a whole byte to save the boolean information. If more than one <code>boolean</code>
	 * value has to be written, consider using {@link #writeFlags(boolean...)} to send up to 8 boolean values
	 * with a single byte.
	 * @param b The <code>boolean</code> value that should be written
	 */
	public default void writeBoolean(boolean b) {
		writeByte(b ? (byte) 1 : (byte) 0); 
	}
	
	/**
	 * Writes a single <code>byte</code> value at the end of the current byte sequence. 
	 * @param b The <code>byte</code> that should be written
	 */
	public void writeByte(byte b);
	
	/**
	 * Writes a <code>char</code> value at the end of the current byte sequence, using two <code>byte</code> values. 
	 * @param c The <code>char</code> that should be written
	 */
	public default void writeChar(char c) {
		writeByte((byte) (c & 0xFF));
		writeByte((byte) ((c >>> 8) & 0xFF));
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
