package dev.lb.simplebase.net.packet;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.util.ThreadsafeIterable;

/**
 * Stores a set of {@link PacketIDMapping}s and provides methods for adding / iterating elements.
 * It does not offer methods to remove mappings as these mapping lists are usually not changed for the entire runtime
 * of the program and are setup only once.
 * <p>
 * All add/find operations are threadsafe. 
 */
@Threadsafe
public class PacketIDMappingProvider {

	protected PacketIDMappingProvider() {
		mappings = new HashSet<>(); //Will be manually synchronized
	}
	
	private final Set<PacketIDMapping> mappings;
	private class Threadsafe implements ThreadsafeIterable<PacketIDMappingProvider, PacketIDMapping> {

		@Override
		public void action(Consumer<PacketIDMappingProvider> action) {
			synchronized (mappings) { //Run the action while holding monitor
				action.accept(PacketIDMappingProvider.this);
			}
		}

		@Override
		public <R> R actionReturn(Function<PacketIDMappingProvider, R> action) {
			synchronized (mappings) { //Run the action while holding monitor
				return action.apply(PacketIDMappingProvider.this); //Return here, will release monitor on its own
			}
		}

		@Override
		public void forEach(Consumer<? super PacketIDMapping> itemAction) {
			synchronized (mappings) { //Iterate backing set while holding the monitor
				mappings.forEach(itemAction);
			}
		}

		@Override
		public Iterator<PacketIDMapping> iterator() {
			if(Thread.holdsLock(mappings)) { //If it is held by this thread then it is held somewhere up the stack -> 
				return mappings.iterator(); //so we can return an instance to the caller (relatively) safely
			} else {
				throw new IllegalStateException("Current thread does not hold object monitor"); //No lock, no iterator
			}
		}

		@Override
		public Spliterator<PacketIDMapping> spliterator() {
			if(Thread.holdsLock(mappings)) { //If it is held by this thread then it is held somewhere up the stack -> 
				return mappings.spliterator(); //so we can return an instance to the caller (relatively) safely
			} else {
				throw new IllegalStateException("Current thread does not hold object monitor"); //No lock, no iterator
			}
		}

		@Override
		public <R> Optional<R> forEachReturn(Function<? super PacketIDMapping, Optional<R>> itemFunction) {
			synchronized (mappings) {
				for(PacketIDMapping mapping : mappings) {
					Optional<R> val = itemFunction.apply(mapping);
					if(val.isPresent()) return val;
				}
				return Optional.empty();
			}
		}
		
	}

	/**
	 * Creates an object that allows threadsafe access to the mapping list.
	 * @return A {@link ThreadsafeIterable} for this provider
	 */
	public ThreadsafeIterable<PacketIDMappingProvider, PacketIDMapping> threadsafe() {
		return new Threadsafe();
	}
	
	/**
	 * Adds a {@link PacketIDMapping} to the collection.
	 * @param mapping The mapping to add
	 * @throws NullPointerException If mapping is null and the implementation does not allow null elements
	 * @throws IllegalArgumentException If a mapping with the same ID or Class already exists in this set and the implementation does not allow duplicate elements
	 */
	public void addMapping(PacketIDMapping newMapping) {
		//1. Validate before getting the lock
		Objects.requireNonNull(newMapping, "'newMapping' parameter must not be null");
		//2. Sync
		synchronized (mappings) {
			//3. Check for duplicates (While sync, because it iterates to find possible dupes
			for(PacketIDMapping id : mappings) {
				if(id == null) continue; //Shouldn't really be possible, but check anyways
				if(id.getPacketID() == newMapping.getPacketID()) { //If ID is the same
					throw new IllegalArgumentException(
							"PacketIDMappingContainer implementation does not allow any duplicates (ID=" + id.getPacketID() + ")");
				} else if(id.getPacketClass() == newMapping.getPacketClass()) { //Or class
					throw new IllegalArgumentException(
							"PacketIDMappingContainer implementation does not allow any duplicates (Class=" + id.getPacketClass().getCanonicalName() + ")");
				}
			}
			//4. If no exception was thrown by now, the mapping can be inserted
			mappings.add(newMapping);
		} //5. release the lock
	}

	/**
	 * Adds several {@link PacketIDMapping} to the collection.<br>
	 * If the method exits with an exception, some of the mappings may have been inserted while others have not.<br>
	 * Iteration over the array (or varargs array) will not be synchronized for that array, but thread safety of this object is guaranteed.
	 * @param mappings The mappings to add
	 * @throws NullPointerException If one of the mappings is null and the implementation does not allow null elements
	 * @throws IllegalArgumentException If a mapping with the same ID or Class as one those to add already exists in this set and the implementation does not allow duplicate elements
	 */
	public void addMappings(PacketIDMapping[] newMappings) {
		//1. Validate before getting the lock
		Objects.requireNonNull(newMappings, "'mapping' parameter must not be null");
		//Also, if length==0, we don't do anything
		if(newMappings.length == 0) return;

		//2. Sync
		synchronized (mappings) {
			//3. Do the add operation for every item in the array
			for(PacketIDMapping newMapping : newMappings) {
				//This will lock again, but monitors are reentrant and the locking in there will be (almost) instant because we already have the monitor
				addMapping(newMapping); //that method iterates again every time... nice O(n²) . . . [actually O(m*n)]
			}
		} //4. release the lock
	}
	
	/**
	 * Adds several {@link PacketIDMapping} to the collection.<br>
	 * If the method exits with an exception, some of the mappings may have been inserted while others have not.<br>
	 * Iteration over the {@link Iterable} will only be be synchronized for that iterable
	 * if it implements {@link ThreadsafeIterable}, but thread safety of this object is guaranteed.
	 * @param otherContainer The mappings to add
	 * @throws NullPointerException If one of the mappings is null and the implementation does not allow null elements
	 * @throws IllegalArgumentException If a mapping with the same ID or Class as one those to add already exists in this set and the implementation does not allow duplicate elements
	 */
	public void addMappings(Iterable<PacketIDMapping> otherContainer) {
		//1. Validate before getting the lock
		Objects.requireNonNull(otherContainer, "'otherContainer' parameter must not be null");

		//2. Synchronize
		synchronized (mappings) {
			//ThreadsafeIterable or not?
			if(otherContainer instanceof ThreadsafeIterable) {
				@SuppressWarnings("unchecked") //Hopefully this is ok, I don't really like generic wildcards
				ThreadsafeIterable<? extends ThreadsafeIterable<?, PacketIDMapping>, PacketIDMapping> newMappings
				= (ThreadsafeIterable<? extends ThreadsafeIterable<?, PacketIDMapping>, PacketIDMapping>) otherContainer;
				newMappings.forEach(this::addMapping); // Do this while holding the lock on the iterable
			} else {
				//Do a normal itertaion
				for(PacketIDMapping newMapping : otherContainer) {
					addMapping(newMapping);
				}
			}
		} //3. Release the lock
	}
	
	/**
	 * Adds all mappings that are defined as enum constants in that class.<br>
	 * If the method exits with an exception, some of the mappings may have been inserted while others have not.<br>
	 * Iteration over the set of enum constants does not require any threadsafety because to the constant pool
	 * is immutable at runtime. Thread safety of this object is guaranteed.
	 * @param <T> The type of the enum
	 * @param enumContainer The {@link Class} object for the enum that contains the mappings
	 * @throws IllegalArgumentException If a mapping with the same ID or Class as one those in the enum already exists in this set and the implementation does not allow duplicate elements
	 */
	public <T extends Enum<T> & PacketIDMapping> void addMappings(Class<T> enumContainer) {
		//1. Validate not null. Class will always be an enum because of generic constraints
		Objects.requireNonNull(enumContainer, "'enumContainer' parameter must not be null");
		//2. Read all enum values into the set
		final EnumSet<T> enumSet = EnumSet.allOf(enumContainer);
		//Add all of them, no sync on the enumSet needed
		//The compiler gets really confused at this point when you try to call addMappings(Iterable<>), so we just iterate manually
		//3. Sync
		synchronized (mappings) {
			for(PacketIDMapping newMapping : enumSet) {
				addMapping(newMapping);
			}
		}// release	
	}
	
	/**
	 * Checks whether this container has a mapping with the same numerical ID <b>AND</b> packet class as the one in the partameter.
	 * @param mapping The {@link PacketIDMapping} to check
	 * @return {@code true} if an identical mapping was found, {@code false} otherwise
	 * @see #hasAnyMapping(PacketIDMapping)
	 */
	public boolean hasExactMapping(PacketIDMapping mapping) {
		//1. Validate null
		Objects.requireNonNull(mapping, "'mapping' parameter must not be null");
		//2. Sync
		synchronized (mappings) {
			//3. Iterate and check for the same ID and mapping
			for(PacketIDMapping id : mappings) {
				if(id == null) continue; //Shouldn't really be possible, but check anyways
				if(id.equals(mapping)) return true; //equals ignores the source lambda, comparing that makes little sense
			}
		} //4. release lock
		return false;
	}
	
	/**
	 * Checks whether this container has a mapping with the same numerical ID <b>OR</b> packet class as the one in the partameter.
	 * @param mapping The {@link PacketIDMapping} to check
	 * @return {@code true} if a mapping with the same ID or class was found, {@code false} otherwise
	 * @see #hasExactMapping(PacketIDMapping)
	 */
	public boolean hasAnyMapping(PacketIDMapping mapping) {
		//1. Validate null
		Objects.requireNonNull(mapping, "'mapping' parameter must not be null");
		//2. Sync
		synchronized (mappings) {
			//3. Iterate and check for the same ID and mapping
			for(PacketIDMapping id : mappings) {
				if(id == null) continue; //Shouldn't really be possible, but check anyways
				if(id.getPacketClass() == mapping.getPacketClass() || //OR is intentional, hasAnyMapping checks for ANY similarity
						id.getPacketID() == mapping.getPacketID()) return true; //equals ignores the source lambda, comparing that makes little sense
			}
		} //4. release lock
		return false;
	}

	/**
	 * Finds a mapping that maps the requested class to a numerical ID
	 * @param packetClass The packet class that the mapping should have
	 * @return The matching {@link PacketIDMapping}, or {@code null} of nothing was found
	 */
	public PacketIDMapping findMapping(Class<? extends Packet> packetClass) {
		//1. Validate null
		Objects.requireNonNull(packetClass, "'packetClass' parameter must not be null");
		//2. Sync
		synchronized (mappings) {
			//3. Iterate to find a matching one
			for(PacketIDMapping id : mappings) {
				if(id.getPacketClass() == packetClass) return id;
			}
		}
		//4. return null of nothing was found
		return null;
	}
	
	/**
	 * Finds a mapping that maps the requested ID to a packet class/instance
	 * @param packetID The packet ID that the mapping should have
	 * @return The matching {@link PacketIDMapping}, or {@code null} of nothing was found
	 */
	public PacketIDMapping findMapping(int packetID) {
		//1. Don't validate null for int
		//2. Sync
		synchronized (mappings) {
			//3. Iterate to find a matching one
			for(PacketIDMapping id : mappings) {
				if(id.getPacketID() == packetID) return id;
			}
		}
		//4. return null of nothing was found
		return null;
	}

}
