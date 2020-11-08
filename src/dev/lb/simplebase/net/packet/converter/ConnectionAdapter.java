package dev.lb.simplebase.net.packet.converter;

import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.format.NetworkPacketFormats;

/**
 * A {@link ConnectionAdapter} can receive decoded network packets.
 */
public interface ConnectionAdapter {

	/**
	 * Called when the {@link NetworkPacketFormats#LOGIN} network packet is received.
	 */
	public void receiveUdpLogin();
	/**
	 * Called when the {@link NetworkPacketFormats#LOGOUT} network packet is received.
	 */
	public void receiveUdpLogout();
	/**
	 * Called when the {@link NetworkPacketFormats#PACKET} network packet is received.
	 * @param packet The attached payload
	 */
	public void receivePacket(Packet packet);
	/**
	 * Called when the {@link NetworkPacketFormats#CHECK} network packet is received.
	 * @param uuid The attached payload
	 */
	public void receiveCheck(int uuid);
	/**
	 * Called when the {@link NetworkPacketFormats#CHECKREPLY} network packet is received.
	 * @param uuid The attached payload
	 */
	public void receiveCheckReply(int uuid);
	/**
	 * Called when the {@link NetworkPacketFormats#SERVERINFORQ} network packet is received.
	 */
	public void receiveServerInfoRequest();
	/**
	 * Called when the {@link NetworkPacketFormats#SERVERINFOAN} network packet is received.
	 * @param packet The attached payload
	 */
	public void receiveServerInfoPacket(Packet packet);
	/**
	 * Called when the {@link NetworkPacketFormats#CONNECTED} network packet is received.
	 */
	public void receiveConnectionAccepted();
	
}
