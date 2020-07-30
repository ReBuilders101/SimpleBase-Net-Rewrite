package dev.lb.simplebase.net.config;

public class ServerConfig extends CommonConfig {

	private static final boolean REGISTER_INTERNAL_DEFAULT = true;
	private static final boolean ALLOW_DETECTION_DEFAULT = true;
	private static final ServerType SERVER_TYPE_DEFAULT = ServerType.DEFAULT;
	
	private boolean registerInternalServer;
	private boolean allowDetection;
	private ServerType serverType;
	
	public ServerConfig() {
		this.registerInternalServer = REGISTER_INTERNAL_DEFAULT;
		this.allowDetection = ALLOW_DETECTION_DEFAULT;
		this.serverType = SERVER_TYPE_DEFAULT;
	}
	
	public boolean getRegisterInternalServer() {
		return registerInternalServer;
	}
	
	public void setRegisterInternalServer(boolean value) {
		checkLocked();
		this.registerInternalServer = value;
	}
	
	public boolean getAllowDetection() {
		return allowDetection;
	}
	
	public void setAllowDetection(boolean value) {
		checkLocked();
		this.allowDetection = value;
	}
	
	public ServerType getServerType() {
		return serverType;
	}
	
	public void setServerType(ServerType value) {
		checkLocked();
		this.serverType = value;
	}
}
