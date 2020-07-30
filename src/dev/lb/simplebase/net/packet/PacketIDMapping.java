package dev.lb.simplebase.net.packet;

import java.util.Objects;
import java.util.function.Supplier;

import dev.lb.simplebase.net.annotation.Immutable;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.annotation.ValueType;

/**
 * A {@link PacketIDMapping} connects a packet class to a numerical ID that is used to
 * identify the type of a packet when it is sent over the network. It also provides a method to create
 * new, uninitialized instances of a packet type.
 * <p>
 * This interface can be implemented by an enum type to collect all mappings of your program in a central place
 */
@ValueType
@Immutable
@Threadsafe
public interface PacketIDMapping {
//This cannot be a class because enums must be able to implement this
	
	/**
	 * Creates a new, uninitialized instance of the packet type represented by this mapping.
	 * The instances will be filled with data through the {@link Packet#readData(lb.simplebase.io.ReadableByteData)}
	 * method after creation. Usually only called by the API itself
	 * @return A new packet instance for this packet type
	 */
	@Internal
	public Packet createNewInstance();
	
	/**
	 * The type of the {@link Packet} described by this mapping, as a {@link Class} object.
	 * The class should be unique for all mappings in the same {@link PacketIDMappingProvider}.
	 * @return The class of the packet represented by this mapping
	 */
	public Class<? extends Packet> getPacketClass();
	
	/**
	 * The numerical ID of the {@link Packet} described by this packet. The ID should be unique for all
	 * mappings in the same {@link PacketIDMappingProvider}.
	 * @return The ID of the packet represented by this mapping 
	 */
	public int getPacketID();
	
	/**
	 * Checks whether a {@link PacketIDMapping} would be unique if inserted into the list.
	 * This means that no other mapping for that value <b>or</b> that class exists.
	 * The other mapping with the same mapping ID must not necessarily have the same packet class
	 * to count as the same (and the other way too).<br>This ensures that for any ID or any class there only exists a single valid mapping.
	 * <p>
	 * This method is not always threadsafe, the iterable can be cuncurrently modified. If the iterable is a
	 * {@link PacketIDMappingProvider}, see {@link PacketIDMappingProvider#hasAnyMapping(PacketIDMapping)}.
	 * @param mappingContainer The list/collection/array that contains the other mappings
	 * @param toInsert The new mapping that should be checked before inserting it.
	 * @return Whether the mapping is unique, that means that no equal mapping exists in the list already.
	 */
	public static boolean isUnique(Iterable<PacketIDMapping> mappingContainer, PacketIDMapping toInsert) {
		Objects.requireNonNull(mappingContainer, "'mappingContainer' parameter must not be null");
		Objects.requireNonNull(toInsert, "'toInsert' parameter must not be null");
		
		//Check every one of them for equality
		for(PacketIDMapping id : mappingContainer) {
			if(id == null) continue;
			if(id.getPacketID() == toInsert.getPacketID() || id.getPacketClass() == toInsert.getPacketClass()) return false;
		}
		return true;
	}
	
	/**
	 * Tries to find a {@link PacketIDMapping} that maps a certain packet class to an ID.
	 * If no matching mapping is found in the list, the method returns {@code null}.
	 * If the list contains more than one matching mapping, the first one found will be returned.
	 * @param mappingContainer The list/collection/array that contains the other mappings
	 * @param packetClass The class of the packet implementation for which the mapping should be found
	 * @return The matching {@link PacketIDMapping}, or {@code null}
	 */
	public static PacketIDMapping find(Iterable<PacketIDMapping> mappingContainer, Class<? extends Packet> packetClass) {
		Objects.requireNonNull(mappingContainer, "'mappingContainer' parameter must not be null");
		Objects.requireNonNull(packetClass, "'packetClass' parameter must not be null");
		
		//Check every one of them for equality
		for(PacketIDMapping id : mappingContainer) {
			if(id == null) continue;
			if(id.getPacketClass() == packetClass) return id;
		}
		return null;
	}
	
	/**
	 * Tries to find a {@link PacketIDMapping} that maps a certain packet ID to an implementation class.
	 * If no matching mapping is found in the list, the method returns {@code null}.
	 * If the list contains more than one matching mapping, the first one found will be returned.
	 * @param mappingContainer The list/collection/array that contains the other mappings
	 * @param packetID The numerical ID of the packet implementation for which the mapping should be found
	 * @return The matching {@link PacketIDMapping}, or {@code null}
	 */
	public static PacketIDMapping find(Iterable<PacketIDMapping> mappingContainer, int packetID) {
		Objects.requireNonNull(mappingContainer, "'mappingContainer' parameter must not be null");
		
		//Check every one of them for equality
		for(PacketIDMapping id : mappingContainer) {
			if(id == null) continue;
			if(id.getPacketID() == packetID) return id;
		}
		return null;
	}
	
	/**
	 * Creates a new instance of {@link PacketIDMapping} using a default implementation.
	 * The returned instance will be threadsafe, but the supplier passed to this method does not have to be, as it is only
	 * called  on one thread at a time by {@link #createNewInstance()}.
	 * @param <T> The packet implementation type.
	 * @param packetID The numerical ID for this packet type
	 * @param packetClass The class of this packet implementation
	 * @param packetConstructor A supplier for uninitialized packet instances, usually a parameterless constructor. Used in {@link PacketIDMapping#createNewInstance()}
	 * @return A new {@link PacketIDMapping} instance
	 */
	public static <T extends Packet> PacketIDMapping create(int packetID, Class<T> packetClass, Supplier<T> packetConstructor) {
		return new PacketIDMappingImpl(packetConstructor, packetClass, packetID);
	}
}
