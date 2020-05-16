package dev.lb.simplebase.net.packet;

import dev.lb.simplebase.net.annotation.ValueType;
import lb.simplebase.net.io.ReadableByteData;
import lb.simplebase.net.io.WritableByteData;

/**
 * A Packet is a container for data that is sent through a NetworkConnection.
 * Implementations handle the conversion to/from byte data.<p>
 * Classes that implement this interface are typically {@link ValueType}s and
 * have a set of members corresponding to the data sent by this packet.
 * <p>
 * For internal connections, {@link #readData(ReadableByteData)} and {@link #writeData(WritableByteData)}
 * might not be called at all, and instead the packet instance itself will be passed to the receiver.
 * Because of this, <b>packet implementations should be immutable</b>.
 * <p>
 * Packet implementations usually provide two constructors: One is public and sets all member values,
 * for a packet that is supposed to be sent over the network, and an internal constructor that creates an
 * uninitialized instance that will be filled with data later through the {@link #readData(ReadableByteData)}
 * method.
 */
@ValueType
public interface Packet {

	/**
	 * Fills in the packet data from the byte data contained in the {@link ReadableByteData} parameter.
	 * This method is only called once (or not at all) for any packet instance. If this method is called,
	 * the packet instance will be uninitalized (in the state provided by the corresponding {@link PacketIDMapping#createNewInstance()} method).
	 * @param data The packet data as bytes
	 */
	public void readData(ReadableByteData data);
	
	/**
	 * Writes the packet data as bytes into the {@link WritableByteData} parameter.
	 * @param data The target container to write data into.
	 */
	public void writeData(WritableByteData data);
	
	/**
	 * Some packet implementations have a constant size in their byte representation, or the 
	 * size can be easily calculated/estimated. In this case the packet serializer already knows the capacity
	 * of the buffer that is required to convert this packet to bytes and can avoid copying the data into a larger array when it runs
	 * out of capacity mid-write.<p>
	 * The {@link WritableByteData} implementation used in that case might not have a dynamic capacity at all, and
	 * when {@link #writeData(WritableByteData)} tries to write more bytes than this method returns, an exception can be thrown by any write operation.
	 * <p>
	 * Using a very large size to ensure that the capacity is not exceeded is not recommended, because that means that to much
	 * memory will be used. If the byte size is unknown, <b>any negative number</b> can be returned and a {@link WritableByteData}
	 * implementation with a dynamic size will be used.
	 * @return The maximum byte size of the packet data, or any negative value for an unknown size
	 */
	public default int getByteSize() {
		return -1;
	}
}
