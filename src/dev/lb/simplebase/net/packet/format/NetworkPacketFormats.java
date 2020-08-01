package dev.lb.simplebase.net.packet.format;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import dev.lb.simplebase.net.io.ByteDataHelper;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.converter.ConnectionAdapter;
import dev.lb.simplebase.net.util.Lazy;

public final class NetworkPacketFormats {
	public static final int PACKET_BUFFER_SIZE = 128;
	
	private NetworkPacketFormats() {}

	/**
	 * Packet format:<br>
	 * 4 bytes typeid<br>
	 * 4 bytes datalength -> n<br>
	 * n bytes data
	 */
	private static final int PACKET_UUID = ByteDataHelper.cInt(new byte[] {'P', 'A', 'C', 'K'});
	public static final NetworkPacketFormat<ConnectionAdapter, PacketIDMappingProvider, Packet> PACKET = 
			new NetworkPacketFormat1Packet<>(PACKET_UUID, "Packet", ConnectionAdapter::receivePacket);
	
	
	/**
	 * Check format:<br>
	 * 4 bytes uuid
	 */
	private static final int CHECK_UUID = ByteDataHelper.cInt(new byte[] {'C', 'H', 'C', 'K'});
	public static final NetworkPacketFormat<ConnectionAdapter, Object, Integer> CHECK =
			new NetworkPacketFormat4Bytes<>(CHECK_UUID, "Check", ConnectionAdapter::receiveCheck);

	
	/**
	 * Check format:<br>
	 * 4 bytes uuid
	 */
	private static final int CHECKREPLY_UUID = ByteDataHelper.cInt(new byte[] {'C', 'H', 'R', 'P'});
	public static final NetworkPacketFormat<ConnectionAdapter, Object, Integer> CHECKREPLY = 
			new NetworkPacketFormat4Bytes<>(CHECKREPLY_UUID, "Check-Reply", ConnectionAdapter::receiveCheckReply);

	
	
	private static final int LOGIN_UUID = ByteDataHelper.cInt(new byte[] {'H', 'E', 'L', 'O'});
	public static final NetworkPacketFormat<ConnectionAdapter, Object, Object> LOGIN =
			new NetworkPacketFormatEmpty<>(LOGIN_UUID, "UDP-Login", ConnectionAdapter::receiveUdpLogin);

	
	private static final int LOGOUT_UUID = ByteDataHelper.cInt(new byte[] {'B', 'Y', 'E', 'X'});
	public static final NetworkPacketFormat<ConnectionAdapter, Object, Object> LOGOUT =
			new NetworkPacketFormatEmpty<>(LOGOUT_UUID, "UDP-Logout", ConnectionAdapter::receiveUdpLogout);
	
	private static final int SERVERINFORQ_UUID = ByteDataHelper.cInt(new byte[] {'S', 'I', 'R', 'Q'});
	public static final NetworkPacketFormat<ConnectionAdapter, Object, Object> SERVERINFORQ =
			new NetworkPacketFormatEmpty<>(SERVERINFORQ_UUID, "Server-Info-Request", ConnectionAdapter::receiveServerInfoRequest);
	
	private static final int SERVERINFOAN_UUID = ByteDataHelper.cInt(new byte[] {'S', 'I', 'A', 'N'});
	public static final NetworkPacketFormat<ConnectionAdapter, PacketIDMappingProvider, Packet> SERVERINFOAN =
			new NetworkPacketFormat1Packet<>(SERVERINFOAN_UUID, "Server-Info-Packet", ConnectionAdapter::receiveServerInfoPacket);
	
	
	private static final Lazy<Set<NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider , ?>>> ALL_FORMATS =
			new Lazy<>(() -> {
				final Set<NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider , ?>> set = new HashSet<>();
				set.add(PACKET);
				set.add(CHECK);
				set.add(CHECKREPLY);
				set.add(LOGIN);
				set.add(LOGOUT);
				set.add(SERVERINFORQ);
				set.add(SERVERINFOAN);
				return Collections.unmodifiableSet(set);
			});
	public static Set<NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider , ?>> allFormats() {
		return ALL_FORMATS.get();
	}
	
	public static NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider, ?> findFormat(int formatId) {
		for(NetworkPacketFormat<ConnectionAdapter, ? super PacketIDMappingProvider, ?> format : allFormats()) {
			if(format.getUniqueIdentifier() == formatId) return format;
		}
		return null;
	}
}
