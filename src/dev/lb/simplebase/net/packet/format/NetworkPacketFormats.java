package dev.lb.simplebase.net.packet.format;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import dev.lb.simplebase.net.io.ByteDataHelper;
import dev.lb.simplebase.net.packet.PacketIDMappingProvider;
import dev.lb.simplebase.net.packet.converter.ConnectionAdapter;
import dev.lb.simplebase.net.util.Lazy;

public final class NetworkPacketFormats {

	private NetworkPacketFormats() {}

	/**
	 * Packet format:<br>
	 * 4 bytes typeid<br>
	 * 4 bytes datalength -> n<br>
	 * n bytes data
	 */
	private static final int PACKET_UUID = ByteDataHelper.cInt(new byte[] {'P', 'A', 'C', 'K'});
	public static final NetworkPacketFormat<ConnectionAdapter, PacketIDMappingProvider, ?> PACKET = 
			new NetworkPacketFormat1Packet<>(PACKET_UUID, ConnectionAdapter::receivePacket);
	
	
	/**
	 * Check format:<br>
	 * 4 bytes uuid
	 */
	private static final int CHECK_UUID = ByteDataHelper.cInt(new byte[] {'C', 'H', 'C', 'K'});
	public static final NetworkPacketFormat<ConnectionAdapter, PacketIDMappingProvider, ?> CHECK =
			new NetworkPacketFormat4Bytes<>(CHECK_UUID, ConnectionAdapter::receiveCheck);

	
	/**
	 * Check format:<br>
	 * 4 bytes uuid
	 */
	private static final int CHECKREPLY_UUID = ByteDataHelper.cInt(new byte[] {'C', 'H', 'R', 'P'});
	public static final NetworkPacketFormat<ConnectionAdapter, PacketIDMappingProvider, ?> CHECKREPLY = 
			new NetworkPacketFormat4Bytes<>(CHECKREPLY_UUID, ConnectionAdapter::receiveCheckReply);

	
	
	private static final int LOGIN_UUID = ByteDataHelper.cInt(new byte[] {'H', 'E', 'L', 'O'});
	public static final NetworkPacketFormat<ConnectionAdapter, PacketIDMappingProvider, ?> LOGIN =
			new NetworkPacketFormatEmpty<>(LOGIN_UUID, ConnectionAdapter::receiveUdpLogin);

	
	private static final int LOGOUT_UUID = ByteDataHelper.cInt(new byte[] {'B', 'Y', 'E', 'X'});
	public static final NetworkPacketFormat<ConnectionAdapter, PacketIDMappingProvider, ?> LOGOUT =
			new NetworkPacketFormatEmpty<>(LOGOUT_UUID, ConnectionAdapter::receiveUdpLogout);
	
	
	
	private static final Lazy<Set<NetworkPacketFormat<ConnectionAdapter, PacketIDMappingProvider , ?>>> ALL_FORMATS =
			new Lazy<>(() -> {
				final Set<NetworkPacketFormat<ConnectionAdapter, PacketIDMappingProvider , ?>> set = new HashSet<>();
				set.add(PACKET);
				set.add(CHECK);
				set.add(CHECKREPLY);
				set.add(LOGIN);
				set.add(LOGOUT);
				return Collections.unmodifiableSet(set);
			});
	public static Set<NetworkPacketFormat<ConnectionAdapter, PacketIDMappingProvider , ?>> allFormats() {
		return ALL_FORMATS.get();
	}
	
	public static NetworkPacketFormat<ConnectionAdapter, PacketIDMappingProvider, ?> findFormat(int formatId) {
		for(NetworkPacketFormat<ConnectionAdapter, PacketIDMappingProvider, ?> format : allFormats()) {
			if(format.getUniqueIdentifier() == formatId) return format;
		}
		return null;
	}
}
