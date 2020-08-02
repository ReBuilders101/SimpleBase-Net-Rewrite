package dev.lb.simplebase.net.packet.converter;

import java.nio.ByteBuffer;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.io.ByteDataHelper;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormat;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;

/**
 * Converts bytes to packets.
 * <br><b>Not Threadsafe!</b><br>
 * Call {@code accept...} methods from only one thread.
 */
public final class ByteToPacketConverter {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("packet-decode");
	
	private final PacketIDMappingProvider provider;
	private final ConnectionAdapter receiver;

	private NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider, ?> currentFormat;
	private ByteBuffer buffer; //Will be ready for put() operations
	private int requiredBytes;

	public ByteToPacketConverter(ConnectionAdapter connection, PacketIDMappingProvider provider) {
		this.receiver = connection;
		this.provider = provider;
		this.currentFormat = null;
		this.buffer = ByteBuffer.allocate(NetworkPacketFormats.PACKET_BUFFER_SIZE);
		this.requiredBytes = 4; //Prepare for format id reading
	}
	
	/**
	 * {@code requiredBytes} will never be 0 after this method returns.
	 */
	private void updateAccumulationState(int receivedAmount) {
		requiredBytes -= receivedAmount;
		if(currentFormat == null) {
			//Here we try to figure out what our next format should be
			if(requiredBytes == 0) { //Should have 4 bytes now
				final int formatId = ByteDataHelper.cInt((ByteBuffer) buffer.asReadOnlyBuffer().flip());
				final NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider, ?> format = NetworkPacketFormats.findFormat(formatId);
				if(format == null) {
					LOGGER.debug("Unexpected bytes: No format found in %x", formatId);
					//No format found
					requiredBytes = 1;
					//Buffer is in put() mode
					buffer.flip(); //read mode
					buffer.get(); //increase position to discard first byte in compact()
					buffer.compact();
					//Will read the next byte
				} else {
					currentFormat = format;
					buffer.clear(); //reset this, reqBytes is 0 for next part
					//the active format will be used in the next if statement
				}
			}
		}
		
		if(currentFormat != null) { //Format is active
			if(requiredBytes == 0) {
				//Ask the format how much more we need
				int required = currentFormat.receiveMore((ByteBuffer) buffer.asReadOnlyBuffer().flip());
				if(required < 0) { //Invalid packet
					//reset state
					LOGGER.debug("Unexpeceted bytes: Data is invalid for format (%s)", currentFormat.getName());
					resetToFindFormat();
				} else if(required > 0) {
					//We need more bytes
					requiredBytes = required;
				} else { //Perfect
					currentFormat.decodeAndPublish(receiver, provider, (ByteBuffer) buffer.asReadOnlyBuffer().flip());
					//When the packet is done, reset
					resetToFindFormat();
				}
			}
		}
	}
	
	private void resetToFindFormat() {
		currentFormat = null;
		buffer.clear();
		requiredBytes = 4; //The format ID
	}
	
	/**
	 * Puts a single byte into the accumulation buffer.
	 * @param data The byte value
	 */
	public void acceptByte(byte data) {
		ensureCapacity(1);
		buffer.put(data);
		updateAccumulationState(1);
	}

	/**
	 * Bulk-puts byte array into the accumulation buffer.
	 * @param data The byte values
	 */
	public void acceptBytes(byte[] data) {
		final int bulkLength = data.length;
		int pointer = 0;
		while(pointer != data.length) {
			//Only put the required amount. Will update requiredBytes.
			final int amount = Math.min(bulkLength - pointer, requiredBytes);
			acceptBytesUnchecked(data, pointer, amount);
			pointer += amount;
		}
	}

	private void acceptBytesUnchecked(byte[] data, int source, int length) {
		ensureCapacity(data.length);
		buffer.put(data);
		updateAccumulationState(data.length);
	}

	/**
	 * Bulk-puts byte array into the accumulation buffer.<br>
	 * The buffer must be ready for relative read operations ({@code flip()} if necessary)
	 * @param data The byte values
	 */
	public void acceptBytes(ByteBuffer data) {
		while(data.remaining() > 0) {
			acceptBytesUnchecked(data, Math.min(data.remaining(), requiredBytes));
		}
	}
	
	private void acceptBytesUnchecked(ByteBuffer data, int amount) {
		ensureCapacity(amount);
		data.put(data);
		updateAccumulationState(amount);
	}

	private void ensureCapacity(int required) {
		if(buffer == null) {
			buffer = ByteBuffer.allocate(Math.max(NetworkPacketFormats.PACKET_BUFFER_SIZE, required));
		} else if(buffer.remaining() < required){
			final int newCapacity = buffer.capacity() + NetworkPacketFormats.PACKET_BUFFER_SIZE * 
					((int) Math.floorDiv(required, NetworkPacketFormats.PACKET_BUFFER_SIZE));
			final ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity); //ready for put() by default
			buffer.flip(); //prepare for get() to copy into the new buffer
			newBuffer.put(buffer);
			buffer = newBuffer;
		}
	}

}
