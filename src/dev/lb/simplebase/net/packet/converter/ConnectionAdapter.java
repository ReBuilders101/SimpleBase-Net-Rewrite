package dev.lb.simplebase.net.packet.converter;

import dev.lb.simplebase.net.packet.Packet;

public interface ConnectionAdapter {

	public void receiveUdpLogin();
	public void receiveUdpLogout();
	public void receivePacket(Packet packet);
	public void receiveCheck(int uuid);
	public void receiveCheckReply(int uuid);
	public void receiveServerInfoRequest();
	public void receiveServerInfoPacket(Packet packet);
	
}
