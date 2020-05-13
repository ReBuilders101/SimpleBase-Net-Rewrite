package dev.lb.simplebase.net;

import dev.lb.simplebase.net.annotation.Threadsafe;
import dev.lb.simplebase.net.packet.PacketIDMappingContainer;

public abstract class NetworkManagerCommon {

	@Threadsafe
	public final PacketIDMappingContainer MappingContainer;
	
	protected NetworkManagerCommon() {
		MappingContainer = new PacketIDMappingContainerImpl();
	}
	
	public abstract void addPacketHandler(/*TODO packethandler class*/);
	
}
