package dev.lb.simplebase.net;

import java.nio.ByteBuffer;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.io.ByteDataHelper;
import dev.lb.simplebase.net.io.ReadableByteData;
import dev.lb.simplebase.net.io.read.NIOReadableData;
import dev.lb.simplebase.net.io.write.DynamicArrayWritableData;
import dev.lb.simplebase.net.io.write.DynamicNIOWritableData;
import dev.lb.simplebase.net.io.write.FixedArrayWriteableData;
import dev.lb.simplebase.net.io.write.FixedNIOWritableData;
import dev.lb.simplebase.net.io.write.WritableArrayData;
import dev.lb.simplebase.net.io.write.WritableNIOData;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketIDMapping;
import dev.lb.simplebase.net.packet.PacketIDMappingContainer;

/**
 * Converts between packets and bytes
 */
@Internal
class PacketConverter {

	private final PacketIDMappingContainer mappings;
	private final NetworkConnection connection;
	private final int encodeBufferInitialSize;
	
	protected PacketConverter(NetworkManagerCommon manager, NetworkConnection connection) {
		this.mappings = manager.MappingContainer;
		this.connection = connection;
		this.encodeBufferInitialSize = manager.getConfig().getEncodeBufferInitialSize();
		
		this.accumulateInt = ByteBuffer.allocate(4);
		this.currentPhase = SendingUnitPhase.SEARCH_TYPE;
	}
	
	private final ByteBuffer accumulateInt;
	private SendingUnitPhase currentPhase;
	
	private ByteBuffer accumulateDynamic;
	private int packetClass;
	private int packetLength;
	
	//////////////////////////////////// DECODE
	
	protected void receiveByte(byte data) {
		if(currentPhase.useInt) { //use the int accumulator
			if(accumulateInt.remaining() > 0) { //space left
				accumulateInt.put(data);
			} else {
				throw new IllegalStateException("PacketConverter: Int accumulator is full");
			}
		} else { //Use a dynamic accumulator
			if(accumulateDynamic == null) throw new IllegalStateException("PacketConverter: Dynamic accumulator is not present");
			if(accumulateDynamic.remaining() > 0) {
				accumulateDynamic.put(data);
			} else {
				throw new IllegalStateException("PacketConverter: Dynamic accumulator is full");
			}
		}
		updateState(); //Update state changes
	}
	
	protected void receiveBytes(byte[] data) {
		receiveBytes(ByteBuffer.wrap(data));
	}
	
	protected void receiveBytes(ByteBuffer data) {
		while(data.remaining() > 0) {
			ByteBuffer accumulator;
			if(currentPhase.useInt) {
				accumulator = accumulateInt;
			} else if(accumulateDynamic != null) {
				accumulator = accumulateDynamic;
			} else {
				throw new IllegalStateException("PacketConverter: Dynamic accumulator is not present");
			}
			
			final int remaining = accumulator.remaining();
			if(data.remaining() <= remaining) { //put the whole thing
				accumulator.put(data);
			} else { //put as much as possible
				if(remaining == 0) throw new IllegalStateException("PacketConverter: Accumulator is full");
				//Apparently you can't bulk put parts of the source, so use backing arrays
				accumulator.put(data.array(), data.position(), remaining);
			}
		}
	}
	
	
	private int makeInt() {
		if(accumulateInt.remaining() == 0) {
			accumulateInt.rewind();
			return ByteDataHelper.cInt(accumulateInt);
		} else {
			throw new IllegalStateException("Int not completed: " + accumulateInt.remaining() + " remaining");
		}
	}
	
	//Transition function for this state machine
	private void updateState() {
		//This code is a bit messy even after the rewrite
		if(currentPhase.useInt && accumulateInt.remaining() == 0) {
			int value = makeInt();
			switch (currentPhase) {
			case SEARCH_TYPE:
				SendingUnitType next = SendingUnitType.decode(value);
				switch (next) {
				case PACKET:
					currentPhase = SendingUnitPhase.PACKET_CLASS; //read class next
				case CHECK:
					currentPhase = SendingUnitPhase.CHECK_UUID; //read uuid next
				case CHECK_REPLY:
					currentPhase = SendingUnitPhase.CHECK_REPLY_UUID; //read uuid next
				case UDP_LOGIN:
					//No parameters, put directly
					makeUDPLogin();
					//phase stays the same
				case UDP_LOGOUT:
					makeUDPLogout();
				default: 
					NetworkManager.NET_LOG.warning("Received an unknown sending unit with code " + Integer.toHexString(value));
				}
			case PACKET_CLASS:
				packetClass = value;
				currentPhase = SendingUnitPhase.PACKET_LENGTH; //Read length next
			case PACKET_LENGTH: //Packet length read, switch to data
				packetLength = value;
				accumulateDynamic = ByteBuffer.allocate(value); //make the data buffer
				currentPhase = SendingUnitPhase.PACKET_DATA; //Read data next
			case CHECK_UUID: //check uuid completed
				makeCheck(value);
				currentPhase = SendingUnitPhase.SEARCH_TYPE; //Reset
			case CHECK_REPLY_UUID: //Reply uuid completed 
				makeCheckReply(value);
				currentPhase = SendingUnitPhase.SEARCH_TYPE; //Reset
			default: throw new IllegalStateException("PacketConverter: Invalid phase: " + currentPhase);
			}
		} else if(!currentPhase.useInt && accumulateDynamic.remaining() == 0) {
			//Packet data completed
			if(currentPhase != SendingUnitPhase.PACKET_DATA) throw new IllegalStateException("PacketConverter: Dynamic buffer filled in state " + currentPhase);
			//make the packet and reset
			makePacket(packetClass, packetLength, accumulateDynamic);
			accumulateDynamic = null;
			currentPhase = SendingUnitPhase.SEARCH_TYPE;
		}
	}
	
	private void makePacket(int clazz, int length, ByteBuffer buffer) {
		PacketIDMapping mapping = mappings.findMapping(clazz);
		if(mapping == null) {
			NetworkManager.NET_LOG.warning("PacketIDMapping for " + clazz + " was not found, packet not processed");
		} else {
			Packet instance = mapping.createNewInstance(); //New uninitialized packet
			buffer.flip(); //Has been written to, now read
			ReadableByteData data = new NIOReadableData(buffer);
			instance.readData(data);
			connection.receivePacket(instance);
		}
	}
	
	private void makeCheck(int uuid) {
		connection.receiveConnectionCheck(uuid);
	}
	
	private void makeCheckReply(int uuid) {
		connection.receiveConnectionCheckReply(uuid);
	}
	
	private void makeUDPLogin() {
		throw new UnsupportedOperationException("Login can't be sent to an established connection");
	}
	
	private void makeUDPLogout() {
		connection.receiveUDPLogout();
	}
	
	////////////////////////////////// ENCODE
	
	public byte[] packetToArray(Packet packet) {
		final int packetLength = packet.getByteSize();
		final WritableArrayData data;
		if(packetLength < 0) { //Dynamic
			data = new DynamicArrayWritableData(encodeBufferInitialSize);
		} else { //Fixed
			data = new FixedArrayWriteableData(packetLength);
		}
		packet.writeData(data);
		return data.getArray();
	}
	
	/**
	 * The Buffer will always have a backing array
	 */
	public ByteBuffer packetToBuffer(Packet packet) {
		final int packetLength = packet.getByteSize();
		final WritableNIOData data;
		if(packetLength < 0) { //Dynamic
			data = new DynamicNIOWritableData(encodeBufferInitialSize);
		} else { //Fixed
			data = new FixedNIOWritableData(packetLength);
		}
		packet.writeData(data);
		return data.getBuffer();
	}
	
	public byte[] checkToArray(int uuid) {
		final byte[] array = new byte[8];
		ByteDataHelper.cInt(SendingUnitType.CHECK.code, array, 0);
		ByteDataHelper.cInt(uuid, array, 4);
		return array;
	}
	
	public ByteBuffer checkToBuffer(int uuid) {
		final ByteBuffer buffer = ByteBuffer.allocate(8);
		ByteDataHelper.cInt(SendingUnitType.CHECK.code, buffer);
		ByteDataHelper.cInt(uuid, buffer);
		buffer.rewind();
		return buffer;
	}
	
	public byte[] checkReplyToArray(int uuid) {
		final byte[] array = new byte[8];
		ByteDataHelper.cInt(SendingUnitType.CHECK_REPLY.code, array, 0);
		ByteDataHelper.cInt(uuid, array, 4);
		return array;
	}
	
	public ByteBuffer checkReplyToBuffer(int uuid) {
		final ByteBuffer buffer = ByteBuffer.allocate(8);
		ByteDataHelper.cInt(SendingUnitType.CHECK_REPLY.code, buffer);
		ByteDataHelper.cInt(uuid, buffer);
		buffer.rewind();
		return buffer;
	}
	
	public byte[] udpLoginToArray() {
		final byte[] array = new byte[4];
		ByteDataHelper.cInt(SendingUnitType.UDP_LOGIN.code, array, 0);
		return array;
	}
	
	public ByteBuffer udpLoginToBuffer() {
		final ByteBuffer buffer = ByteBuffer.allocate(8);
		ByteDataHelper.cInt(SendingUnitType.UDP_LOGIN.code, buffer);
		buffer.rewind();
		return buffer;
	}
	
	public byte[] udpLogoutToArray() {
		final byte[] array = new byte[4];
		ByteDataHelper.cInt(SendingUnitType.UDP_LOGOUT.code, array, 0);
		return array;
	}
	
	public ByteBuffer udpLogoutToBuffer() {
		final ByteBuffer buffer = ByteBuffer.allocate(8);
		ByteDataHelper.cInt(SendingUnitType.UDP_LOGOUT.code, buffer);
		buffer.rewind();
		return buffer;
	}
	
	private static enum SendingUnitPhase {
		SEARCH_TYPE(true),
		PACKET_CLASS(true), PACKET_LENGTH(true), PACKET_DATA(false),
		CHECK_UUID(true),
		CHECK_REPLY_UUID(true);
		
		private final boolean useInt;
		
		private SendingUnitPhase(boolean useInt) {
			this.useInt = useInt;
		}
	}
	
	private static enum SendingUnitType {
		PACKET(0xFEDCBA00), CHECK(0xFEDCBA01), CHECK_REPLY(0xFEDCBA02),
		UDP_LOGIN(0xFEDCBA03), UDP_LOGOUT(0xFEDCBA04), UNKNOWN(0x00000000);
		
		private final int code;
		
		private SendingUnitType(int code) {
			this.code = code;
		}
		
		public static SendingUnitType decode(int type) {
			for(SendingUnitType t : values()) {
				if(t.code == type) return t;
			}
			return UNKNOWN;
		}
	}
	
}
