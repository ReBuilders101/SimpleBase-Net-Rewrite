package dev.lb.simplebase.net.packet;

import java.util.Objects;
import java.util.function.Supplier;

import dev.lb.simplebase.net.annotation.Immutable;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.annotation.ValueType;

@ValueType
@Internal
@Threadsafe
@Immutable
class PacketIDMappingImpl implements PacketIDMapping {

	private final Supplier<? extends Packet> packetSource;
	private final Class<? extends Packet> packetClass;
	private final int packetID;
		
	protected PacketIDMappingImpl(Supplier<? extends Packet> packetConstructor, Class<? extends Packet> packetClass, int packetID) {
		this.packetSource = packetConstructor;
		this.packetClass = packetClass;
		this.packetID = packetID;
	}

	@Override
	public Packet createNewInstance() {
		final Packet newPacket;
		synchronized (packetSource) { //Source may not be threadsafe, so it will only be called once at a time
			newPacket = Objects.requireNonNull(packetSource.get(), () -> 
				"PacketIDMapping supplier returned null when a new packet instance was requested |IN: " + toString());
		}
		//Once we have the instance, we can validate parallel
		if(newPacket.getClass() != packetClass) throw new ClassCastException("PacketIDMapping supplier returned a packet with class '" +
				newPacket.getClass().getSimpleName() + "', mapping expected class '" + packetClass.getSimpleName() + "' |IN: " + toString());
		return newPacket;
	}

	@Override
	public Class<? extends Packet> getPacketClass() {
		return packetClass;
	}

	@Override
	public int getPacketID() {
		return packetID;
	}

	@Override
	public int hashCode() {
		return Objects.hash(packetClass, packetID); //Source is not considered for hashCode and equals
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PacketIDMappingImpl)) {
			return false;
		}
		PacketIDMappingImpl other = (PacketIDMappingImpl) obj;
		return Objects.equals(packetClass, other.packetClass) && packetID == other.packetID; //Source is not considered for hashCode and equals
	}

	@Override
	public String toString() {
		return "PacketIDMappingImpl [packetSource=" + packetSource + ", packetClass=" + packetClass + ", packetID="
				+ packetID + "]";
	}
	
}
