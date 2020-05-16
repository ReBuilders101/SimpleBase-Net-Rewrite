package dev.lb.simplebase.net;

import java.net.SocketAddress;

import dev.lb.simplebase.net.annotation.StaticType;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.log.Formatter;
import dev.lb.simplebase.net.log.LogLevel;
import dev.lb.simplebase.net.log.Loggers;

@StaticType
public final class NetworkManager {
	
	private NetworkManager() {/*No instantiation possible, contains only static utility methods*/}
	
	/**
	 * The {@link AbstractLogger} used by the API to give information to the user.<br>
	 * Call {@link AbstractLogger#setLogLevel(AbstractLogLevel)} to set detail level.
	 */
	public static final AbstractLogger NET_LOG = Loggers.printSysOut(LogLevel.METHOD, Formatter.getComplex(
					Formatter.getStaticText("Net-Simplebase"),
					Formatter.getLogLevel(),
					Formatter.getCurrentTime(),
					Formatter.getThreadName(),
					Formatter.getDefault()));
	
	public static void main(String[] args) {
		NetworkConnection con = (NetworkConnection) new Object();
		
		//OK, state is read once and not relevan afterward
		System.out.println(con.getCurrentState());
		
		//NOT OK: state may be outdated as soon as the condition is checked
		if(con.getCurrentState() == NetworkConnectionState.OPEN) {
			con.closeConnection();
		}
		
		//INSTEAD: lock the state while comparing and using results
		con.action((c) -> {
			if(c.getThreadsafeState() == NetworkConnectionState.OPEN) {
				c.closeConnection();
			}
		}); 
		
		//NOT OK: [WITH VALUE] state may be outdated as soon as the condition is checked
		boolean res1 = false;
		if(con.getCurrentState() == NetworkConnectionState.OPEN) {
			con.closeConnection();
			res1 = true;
		}

		//INSTEAD: [WITH VALUE] lock the state while comparing and using results
		boolean res2 = con.actionReturn((c) -> {
			if(c.getThreadsafeState() == NetworkConnectionState.OPEN) {
				c.closeConnection();
				return true;
			}
			return false;
		}); 
		
		NetworkID id = con.getLocalID();
		
		SocketAddress address = id.getFunction(NetworkIDFunction.CONNECT);
	}
	
}
