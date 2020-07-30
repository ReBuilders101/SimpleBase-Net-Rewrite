package dev.lb.simplebase.net.packet;

import java.util.concurrent.locks.Lock;
import dev.lb.simplebase.net.ThreadsafeIterable;
import dev.lb.simplebase.net.annotation.Threadsafe;

/**
 * Stores a set of {@link PacketIDMapping}s and provides methods for adding / iterating elements.
 * It does not offer methods to remove mappings as these mapping lists are usually not changed for the entire runtime
 * of the program and are setup only once.
 * <p>
 * All add/find operations are threadsafe. 
 */
@Threadsafe
public interface PacketIDMappingProvider extends ThreadsafeIterable<PacketIDMappingProvider, PacketIDMapping> {
	
	/**
	 * Adds a {@link PacketIDMapping} to the collection.
	 * @param mapping The mapping to add
	 * @throws NullPointerException If mapping is null and the implementation does not allow null elements
	 * @throws IllegalArgumentException If a mapping with the same ID or Class already exists in this set and the implementation does not allow duplicate elements
	 */
	public void addMapping(PacketIDMapping mapping);

	/**
	 * Adds several {@link PacketIDMapping} to the collection.<br>
	 * If the method exits with an exception, some of the mappings may have been inserted while others have not.<br>
	 * Iteration over the array (or varargs array) will not be synchronized for that array, but thread safety of this object is guaranteed.
	 * @param mappings The mappings to add
	 * @throws NullPointerException If one of the mappings is null and the implementation does not allow null elements
	 * @throws IllegalArgumentException If a mapping with the same ID or Class as one those to add already exists in this set and the implementation does not allow duplicate elements
	 */
	public void addMappings(PacketIDMapping[] mappings);
	
	/**
	 * Adds several {@link PacketIDMapping} to the collection.<br>
	 * If the method exits with an exception, some of the mappings may have been inserted while others have not.<br>
	 * Iteration over the {@link Iterable} will only be be synchronized for that iterable
	 * if it implements {@link ThreadsafeIterable}, but thread safety of this object is guaranteed.
	 * @param otherContainer The mappings to add
	 * @throws NullPointerException If one of the mappings is null and the implementation does not allow null elements
	 * @throws IllegalArgumentException If a mapping with the same ID or Class as one those to add already exists in this set and the implementation does not allow duplicate elements
	 */
	public void addMappings(Iterable<PacketIDMapping> otherContainer); 
	
	/**
	 * Adds several {@link PacketIDMapping} to the collection while ensuring thread safety of the source.<br>
	 * If the method exits with an exception, some of the mappings may have been inserted while others have not.<br>
	 * Iteration over the {@link Iterable} will only take place while the lock is held by this thread.
	 * Thread safety of this object is guaranteed.
	 * @param otherContainer The mappings to add
	 * @param containerLock An external {@link Lock} to synchronize on the source iterable
	 * @throws NullPointerException If one of the mappings is null and the implementation does not allow null elements
	 * @throws IllegalArgumentException If a mapping with the same ID or Class as one those to add already exists in this set and the implementation does not allow duplicate elements
	 */
	public void addMappings(Iterable<PacketIDMapping> otherContainer, Lock containerLock); 
	
	/**
	 * Adds all mappings that are defined as enum constants in that class.<br>
	 * If the method exits with an exception, some of the mappings may have been inserted while others have not.<br>
	 * Iteration over the set of enum constants does not require any threadsafety because to the constant pool
	 * is immutable at runtime. Thread safety of this object is guaranteed.
	 * @param <T> The type of the enum
	 * @param enumContainer The {@link Class} object for the enum that contains the mappings
	 * @throws IllegalArgumentException If a mapping with the same ID or Class as one those in the enum already exists in this set and the implementation does not allow duplicate elements
	 */
	public <T extends Enum<T> & PacketIDMapping> void addMappings(Class<T> enumContainer);
	
	/**
	 * Checks whether this container has a mapping with the same numerical ID <b>AND</b> packet class as the one in the partameter.
	 * @param mapping The {@link PacketIDMapping} to check
	 * @return {@code true} if an identical mapping was found, {@code false} otherwise
	 * @see #hasAnyMapping(PacketIDMapping)
	 */
	public boolean hasExactMapping(PacketIDMapping mapping);
	
	/**
	 * Checks whether this container has a mapping with the same numerical ID <b>OR</b> packet class as the one in the partameter.
	 * @param mapping The {@link PacketIDMapping} to check
	 * @return {@code true} if a mapping with the same ID or class was found, {@code false} otherwise
	 * @see #hasExactMapping(PacketIDMapping)
	 */
	public boolean hasAnyMapping(PacketIDMapping mapping);
	
	/**
	 * Finds a mapping that maps the requested class to a numerical ID
	 * @param packetClass The packet class that the mapping should have
	 * @return The matching {@link PacketIDMapping}, or {@code null} of nothing was found
	 */
	public PacketIDMapping findMapping(Class<? extends Packet> packetClass);
	
	/**
	 * Finds a mapping that maps the requested ID to a packet class/instance
	 * @param packetID The packet ID that the mapping should have
	 * @return The matching {@link PacketIDMapping}, or {@code null} of nothing was found
	 */
	public PacketIDMapping findMapping(int packetID);

}
