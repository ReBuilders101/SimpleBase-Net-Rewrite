package dev.lb.simplebase.net.manager;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.config.ClientConfig;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.id.NetworkID;

public final class ManagerInstanceProvider {

	private static final ManagerInstanceProvider INSTANCE = new ManagerInstanceProvider();
	
	public NetworkManagerClient createClient(NetworkID local, NetworkID remote, ClientConfig config) {
		return new NetworkManagerClient(local, remote, config);
	}
	
	public InternalNetworkManagerServer createInternalServer(NetworkID local, ServerConfig config) {
		return new InternalNetworkManagerServer(local, config);
	}
	
	/**
	 * Gives access to {@link NetworkManagerCommon} implementation constructors.<p>
	 * <b>Caller-Sensitive:</b> will throw a {@link RuntimeException} unless the caller is
	 * in the {@link NetworkManager} class
	 * @return The instance provider
	 */
	@Internal
	public static ManagerInstanceProvider get() {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		if(elements.length <= 2) throw new RuntimeException("Unexpected thread state");
		if(elements[2].getClassName() != NetworkManager.class.getName()) throw new RuntimeException("Unexpecetd caller");
		return INSTANCE;
	}
	
}
