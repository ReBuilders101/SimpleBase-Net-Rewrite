package dev.lb.simplebase.net;

import dev.lb.simplebase.net.annotation.InstanceType;
import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.packet.PacketIDMappingContainer;

@InstanceType
public abstract class NetworkManagerCommon {

	@Threadsafe
	public final PacketIDMappingContainer MappingContainer;
	
	protected NetworkManagerCommon() {
		MappingContainer = new PacketIDMappingContainerHash();
	}
	
	public abstract void addPacketHandler(/*TODO packethandler class*/);
	
}
