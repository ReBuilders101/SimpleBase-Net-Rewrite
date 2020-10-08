package dev.lb.simplebase.net.packet.converter;

import java.nio.ByteBuffer;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.io.ByteDataHelper;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.manager.NetworkManagerProperties;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormat;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;

public class ByteAccumulator {
static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("packet-decode");
	
	private final ConnectionAdapter receiver;
	private final int bufferSize;
	private final NetworkManagerProperties managerLike;
	private final ByteToPacketConverter converter;
	
	private NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider, ?> currentFormat;
	private ByteBuffer buffer; //Will be ready for put() operations
	private int requiredBytes;

	public ByteAccumulator(NetworkManagerProperties manager, ConnectionAdapter connection) {
		this.receiver = connection;
		this.bufferSize = manager.getConfig().getPacketBufferInitialSize();
		this.managerLike = manager;
		this.currentFormat = null;
		this.converter = manager.createToPacketConverter();
		this.buffer = ByteBuffer.allocate(bufferSize);
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
					packetReady();
				}
			}
		}
	}
	
	private void packetReady() {
		final ByteBuffer readOnly = (ByteBuffer) buffer.asReadOnlyBuffer().flip();
		if(managerLike.getDecoderPool().isValidCoderThread()) {
			converter.convertAndPublish(readOnly, currentFormat, receiver);
		} else {
			managerLike.getDecoderPool().decodeAndSendPacket(receiver, converter, currentFormat, readOnly);
		}
		resetToFindFormat();
	}
	
	public void resetToFindFormat() {
		currentFormat = null;
		buffer.clear();
		requiredBytes = 4; //The format ID
	}
	
	public boolean isDone() {
		return currentFormat == null && buffer.position() == 0;
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
		while(pointer < data.length) {
			//Only put the required amount. Will update requiredBytes.
			final int amount = Math.min(bulkLength - pointer, requiredBytes);
			acceptBytesUnchecked(data, pointer, amount);
			pointer += amount;
		}
	}

	private void acceptBytesUnchecked(byte[] data, int source, int length) {
		ensureCapacity(length);
		buffer.put(data, source, length);
		updateAccumulationState(length);
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
	
	public ConnectionAdapter getConnectionAdapter() {
		return receiver;
	}
	
	private void acceptBytesUnchecked(ByteBuffer data, int amount) {
		ensureCapacity(amount);
		final ByteBuffer slice = (ByteBuffer) data.slice().limit(amount);
		buffer.put(slice);
		data.position(data.position() + amount);
		updateAccumulationState(amount);
	}

	private void ensureCapacity(int required) {
		if(buffer == null) {
			buffer = ByteBuffer.allocate(Math.max(bufferSize, required));
		} else if(buffer.remaining() < required) {
			final int newCapacity = buffer.capacity() + bufferSize * 
					((int) Math.floorDiv(required, bufferSize));
			final ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity); //ready for put() by default
			buffer.flip(); //prepare for get() to copy into the new buffer
			newBuffer.put(buffer);
			buffer = newBuffer;
		}
	}
}
