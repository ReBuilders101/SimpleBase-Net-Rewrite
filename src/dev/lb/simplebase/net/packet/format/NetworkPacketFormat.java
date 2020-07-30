package dev.lb.simplebase.net.packet.format;

import java.nio.ByteBuffer;

/**
 * A pattern to decode bytes into packet-representing objects
 *
 * @param <Connection> The next stage of handling that will receive the data
 * @param <Data> The object produced by decoding
 */
public abstract class NetworkPacketFormat<Connection, DecodeContext, Data> {	
	
	private final int uuid;
	private final String name;
	
	protected NetworkPacketFormat(int uuid, String name) {
		this.uuid = uuid;
		this.name = name;
	}
	
	/**
	 * The name of this format. Should be short and descriptive, but 
	 * is not required to be unique
	 * @return The name of this format
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * An Integer that is unique to this format. Used by the decoder stage to find the right format for a byte sequence
	 * @return The unique identifier
	 */
	public int getUniqueIdentifier() {
		return uuid;
	}
	
	/**
	 * Queries how many more bytes have to be read, depending on the currently read bytes.
	 * <p>
	 * If the return value is negative, the packet is considered invalid and will be discarded.<br>
	 * If the return value is zero, the packet is complete and the {@link #decode(ByteBuffer)} method
	 * will be called with the data (It might be called at somepoint inbetween as well).<br>
	 * If the return value is positive, this method will be re-called after that amount of bytes has
	 * been accumulated in the buffer.
	 * <p>
	 * The buffer will have a position of 0. The limit is the amount of bytes available.
	 * The capacity and data after the limit is undefined. The position and mark may be modified.<br>
	 * <b>The buffer must be treated as read-only!</b>
	 * @param currentBuffer The currently available bytes. Ready for relative get() metods
	 * @param length the total amount of bytes (independent of how relative gets modify the value of {@link ByteBuffer#remaining()}).
	 * @return How many more bytes have to be received to re-check
	 */
	public abstract int receiveMore(ByteBuffer currentBuffer, int length);
	
	/**
	 * Queries how many more bytes have to be read, depending on the currently read bytes.
	 * <p>
	 * If the return value is negative, the packet is considered invalid and will be discarded.<br>
	 * If the return value is zero, the packet is complete and the {@link #decode(ByteBuffer)} method
	 * will be called with the data (It might be called at somepoint inbetween as well).<br>
	 * If the return value is positive, this method will be re-called after that amount of bytes has
	 * been accumulated in the buffer.
	 * <p>
	 * The buffer will have a position of 0. The limit is the amount of bytes available.
	 * The capacity and data after the limit is undefined. The position and mark may be modified.<br>
	 * <b>The buffer must be treated as read-only!</b>
	 * @param currentBuffer The currently available bytes
	 * @return How many more bytes have to be received to re-check
	 */
	public int receiveMore(ByteBuffer currentBuffer) {
		return receiveMore(currentBuffer, currentBuffer.remaining());
	}
	
	/**
	 * Decode a byte sequence into a complete packet
	 * @param allBytes The complete byte buffer that contains all packet data
	 * @param context The context that can contain additional data for decoding
	 * @return The completed packet, or {@code null} for invalid packets
	 */
	protected abstract Data decode(DecodeContext context, ByteBuffer allBytes);
	
	/**
	 * Push a completed data packet to the next handler stage
	 * @param connection The next handler stage, usually a network connnection
	 * @param data The data packet
	 */
	public abstract void publish(Connection connection, Data data);
	
	/**
	 * {@link #decode(ByteBuffer)} and {@link #publish(Object, Object)}
	 * @param connection
	 * @param allBytes
	 */
	public void decodeAndPublish(Connection connection, DecodeContext context, ByteBuffer allBytes) {
		final Data data = decode(context, allBytes);
		if(data != null) publish(connection, data);
	}
	
	/**
	 * Creates a byte buffer with data for the packet. <br>
	 * The returned buffer will be ready for relative read operations
	 * @param context The context that can contain additional data for encoding
	 * @param data The packet to encode
	 * @return The filled {@link ByteBuffer}, or {@code null} if the packet is invalid
	 */
	public abstract ByteBuffer encode(DecodeContext context, Data data);
}
