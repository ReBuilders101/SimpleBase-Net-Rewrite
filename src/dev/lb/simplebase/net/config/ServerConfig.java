package dev.lb.simplebase.net.config;

import java.net.InetSocketAddress;
import java.util.function.Function;
import dev.lb.simplebase.net.packet.Packet;

public class ServerConfig extends CommonConfig<ServerConfig> {

	private static final boolean REGISTER_INTERNAL_DEFAULT = true;
	private static final boolean ALLOW_DETECTION_DEFAULT = true;
	private static final ServerType SERVER_TYPE_DEFAULT = ServerType.DEFAULT;
	
	private boolean registerInternalServer;
	private boolean allowDetection;
	private ServerType serverType;
	private Function<InetSocketAddress, ? extends Packet> serverInfoFactory;
	
	public ServerConfig() {
		this.registerInternalServer = REGISTER_INTERNAL_DEFAULT;
		this.allowDetection = ALLOW_DETECTION_DEFAULT;
		this.serverType = SERVER_TYPE_DEFAULT;
	}
	
	public boolean getRegisterInternalServer() {
		return registerInternalServer;
	}
	
	public ServerConfig setRegisterInternalServer(boolean value) {
		checkLocked();
		this.registerInternalServer = value;
		return this;
	}
	
	public boolean getAllowDetection() {
		return allowDetection;
	}
	
	public ServerConfig setAllowDetection(boolean value) {
		checkLocked();
		this.allowDetection = value;
		return this;
	}
	
	public ServerType getServerType() {
		return serverType;
	}
	
	public ServerConfig setServerType(ServerType value) {
		checkLocked();
		this.serverType = value;
		return this;
	}
	
	public ServerConfig setServerInfoPacket(Function<InetSocketAddress, ? extends Packet> factory) {
		this.serverInfoFactory = factory;
		return this;
	}
	
	public Packet createServerInfoPacket(InetSocketAddress address) {
		if(serverInfoFactory == null) {
			return null;
		} else {
			return serverInfoFactory.apply(address);
		}
	}
}
