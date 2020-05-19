package dev.lb.simplebase.net.config;

public class ServerConfig extends CommonConfig {

	private boolean registerInternalServer;
	private boolean globalConnectionCheck;
	
	public ServerConfig() {
		super();
		this.registerInternalServer = true;
		this.registerInternalServer = true;
	}
	
	public boolean getRegisterInternalServer() {
		return registerInternalServer;
	}
	
	public void setRegisterInternalServer(boolean value) {
		checkLocked();
		this.registerInternalServer = value;
	}
	
	public boolean getGlobalConnectionCheck() {
		return globalConnectionCheck;
	}
	
	public void setGlobalConnectionCheck(boolean value) {
		checkLocked();
		this.globalConnectionCheck = value;
	}
}
