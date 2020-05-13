package dev.lb.simplebase.net;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.log.LogLevel;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketIDMapping;
import dev.lb.simplebase.net.packet.PacketIDMappingContainer;

@Threadsafe
@Internal
class PacketIDMappingContainerImpl implements PacketIDMappingContainer {

	private final Set<PacketIDMapping> mappings;
	
	protected PacketIDMappingContainerImpl() {
		mappings = new HashSet<>(); //Will be manually synchronized
	}

	@Override
	public void iterate(Consumer<? super PacketIDMapping> itemAction) {
		synchronized (mappings) { //Iterate backing set while holding the monitor
			mappings.forEach(itemAction);
		}
	}

	@Override
	public Iterator<PacketIDMapping> threadsafeIterator() {
		if(Thread.holdsLock(mappings)) { //If it is held by this thread then it is held somewhere up the stack -> 
			return mappings.iterator(); //so we can return an instance to the caller (relatively) safely
		} else {
			throw new IllegalStateException("Current thread does not hold object monitor"); //No lock, no iterator
		}
	}

	@Override
	public Spliterator<PacketIDMapping> threadsafeSpliterator() {
		if(Thread.holdsLock(mappings)) { //If it is held by this thread then it is held somewhere up the stack -> 
			return mappings.spliterator(); //so we can return an instance to the caller (relatively) safely
		} else {
			throw new IllegalStateException("Current thread does not hold object monitor"); //No lock, no iterator
		}
	}

	@Override
	public Iterator<PacketIDMapping> iterator() {
		//1. Complain
		NetworkManager.NET_LOG.warning("Acquired unsafe iterator from PacketIDMappingProvider");
		NetworkManager.NET_LOG.stack(LogLevel.DEBUG, "at stacktrace location:");
		//2. Reluctantly return iterator
		return mappings.iterator();
	}

	@Override
	public void action(Consumer<PacketIDMappingContainer> action) {
		synchronized (mappings) { //Run the action while holding monitor
			action.accept(this);
		}
	}

	@Override
	public <R> R actionReturn(Function<PacketIDMappingContainer, R> action) {
		synchronized (mappings) { //Run the action while holding monitor
			return action.apply(this); //Return here, will release monitor on its own
		}
	}

	@Override
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

	@Override
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

	@Override
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
				newMappings.iterate(this::addMapping); // Do this while holding the lock on the iterable
			} else {
				//Do a normal itertaion
				for(PacketIDMapping newMapping : otherContainer) {
					addMapping(newMapping);
				}
			}
		} //3. Release the lock
	}

	@Override
	public void addMappings(Iterable<PacketIDMapping> otherContainer, Lock containerLock) {
		//1. Validate before getting the lock
		Objects.requireNonNull(otherContainer, "'otherContainer' parameter must not be null");
		Objects.requireNonNull(containerLock, "'containerLock' parameter must not be null");

		//2. Synchronize
		synchronized (mappings) {
			//Now we acquire the provided lock
			try {
				containerLock.lock();
				//Do a normal itertaion
				for(PacketIDMapping newMapping : otherContainer) {
					addMapping(newMapping);
				}
			} finally { //Always release the container lock
				containerLock.unlock();
			}
		} //3. Release the lock
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@Override
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