package dev.lb.simplebase.net.connection;

import dev.lb.simplebase.net.ThreadsafeAction;

public abstract class NetworkConnection implements ThreadsafeAction<NetworkConnection> {

	public abstract void openConnection();
	
	public abstract void closeConnection();
	
	public abstract void checkConnection();
	
	public abstract void openConnectionSync() throws InterruptedException;
	
	public abstract void closeConnectionSync() throws InterruptedException;
	
	public abstract void checkConnectionSync() throws InterruptedException;

	public abstract NetworkConnectionState getState();
	
}
