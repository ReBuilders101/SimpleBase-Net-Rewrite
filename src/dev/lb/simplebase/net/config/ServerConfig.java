package dev.lb.simplebase.net.config;

import java.net.InetSocketAddress;
import java.util.function.BiFunction;
import dev.lb.simplebase.net.manager.NetworkManagerServer;
import dev.lb.simplebase.net.packet.Packet;

public class ServerConfig extends CommonConfig {

	private static final boolean REGISTER_INTERNAL_DEFAULT = true;
	private static final boolean ALLOW_DETECTION_DEFAULT = true;
	private static final ServerType SERVER_TYPE_DEFAULT = ServerType.DEFAULT;
	
	private boolean registerInternalServer;
	private boolean allowDetection;
	private ServerType serverType;
	private BiFunction<NetworkManagerServer, InetSocketAddress, ? extends Packet> serverInfoFactory;
	
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
	
	public ServerConfig setServerInfoPacket(BiFunction<NetworkManagerServer, InetSocketAddress, ? extends Packet> factory) {
		this.serverInfoFactory = factory;
		return this;
	}
	
	public Packet createServerInfoPacket(NetworkManagerServer server, InetSocketAddress address) {
		if(serverInfoFactory == null) {
			return null;
		} else {
			return serverInfoFactory.apply(server, address);
		}
	}

	@Override
	public synchronized ServerConfig setDatagramPacketMaxSize(int size) {
		return (ServerConfig) super.setDatagramPacketMaxSize(size);
	}

	@Override
	public synchronized ServerConfig setUseEncoderThreadPool(boolean value) {
		return (ServerConfig) super.setUseEncoderThreadPool(value);
	}

	@Override
	public synchronized ServerConfig setUseDecoderThreadPool(boolean value) {
		return (ServerConfig) super.setUseDecoderThreadPool(value);
	}

	@Override
	public synchronized ServerConfig setUseThreadPools(boolean encoder, boolean decoder) {
		return (ServerConfig) super.setUseThreadPools(encoder, decoder);
	}

	@Override
	public synchronized ServerConfig setCompressionSize(int minPacketSize) {
		return (ServerConfig) super.setCompressionSize(minPacketSize);
	}

	@Override
	public synchronized ServerConfig setConnectionCheckTimeout(int value) {
		return (ServerConfig) super.setConnectionCheckTimeout(value);
	}

	@Override
	public synchronized ServerConfig setUseManagedThread(boolean value) {
		return (ServerConfig) super.setUseManagedThread(value);
	}

	@Override
	public synchronized ServerConfig setPacketBufferInitialSize(int value) {
		return (ServerConfig) super.setPacketBufferInitialSize(value);
	}

	@Override
	public synchronized ServerConfig setGlobalConnectionCheck(boolean value) {
		return (ServerConfig) super.setGlobalConnectionCheck(value);
	}

	@Override
	public ServerConfig deriveServer() {
		return clone();
	}

	@Override
	public ServerConfig clone() {
		return (ServerConfig) super.clone();
	}
}
