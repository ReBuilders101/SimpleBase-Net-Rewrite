package dev.lb.simplebase.net.manager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.config.ServerConfig;
import dev.lb.simplebase.net.connection.ConnectionConstructor;
import dev.lb.simplebase.net.connection.NetworkConnection;
import dev.lb.simplebase.net.events.ConfigureConnectionEvent;
import dev.lb.simplebase.net.events.FilterRawConnectionEvent;
import dev.lb.simplebase.net.id.NetworkID;

@Internal
abstract class ExternalNetworkManagerServer extends NetworkManagerServer {

	protected ExternalNetworkManagerServer(NetworkID local, ServerConfig config) {
		super(local, config);
	}

	protected void acceptIncomingRawTcpConnection(Socket connectedSocket, ConnectionConstructor ctor) {
		LOGGER.debug("Handling incoming TCP connection");
		//Immediately cancel the connection
		final ServerManagerState stateSnapshot = getCurrentState(); //We are not synced here, but if it is STOPPING or STOPPED it can never be RUNNING again
		if(stateSnapshot.ordinal() > ServerManagerState.RUNNING.ordinal()) {
			LOGGER.warning("Declining incoming TCP socket/channel connection because server is already %s", stateSnapshot);
			try {
				connectedSocket.close();
			} catch (IOException e) {
				LOGGER.error("Error while closing a declined TCP socket/channel", e);
			}
			return;
		}

		//Find the address depending on socket implementation
		final SocketAddress remote = connectedSocket.getRemoteSocketAddress();
		final InetSocketAddress remoteAddress;
		if(remote instanceof InetSocketAddress) {
			remoteAddress = (InetSocketAddress) remote;
		} else {
			remoteAddress = new InetSocketAddress(connectedSocket.getInetAddress(), connectedSocket.getPort());
		}

		//post and handle the event
		final FilterRawConnectionEvent event1 = new FilterRawConnectionEvent(remoteAddress, 
				ManagerInstanceProvider.generateNetworkIdName("RemoteId-"));
		getEventDispatcher().post(FilterRawConnection, event1);
		if(event1.isCancelled()) {
			try {
				connectedSocket.close();
			} catch (IOException e) {
				LOGGER.error("Error while closing a declined TCP socket/channel", e);
			}
		} else {
			final NetworkID networkId = NetworkID.createID(event1.getNetworkIdName(), remoteAddress);

			//Next event
			final ConfigureConnectionEvent event2 = new ConfigureConnectionEvent(this, networkId);
			getEventDispatcher().post(ConfigureConnection, event2);


			try {
				final NetworkConnection tcpConnection = ctor.construct(networkId, event2.getCustomObject());

				//This will start the sync. An exclusive lock for this whole method would be too expensive
				if(!addInitializedConnection(tcpConnection)) {
					//Can't connect after all, maybe the server was stopped in the time we created the connection
					LOGGER.warning("Re-Closed an initialized connection: Server was stopped during connection init");
					tcpConnection.closeConnection();
				}
			} catch (IOException e) {
				LOGGER.error("Socked moved to an invalid state while creating connection object", e);
			}
		}
	}
	
	protected void acceptIncomingRawUdpConnection(InetSocketAddress remoteAddress, ConnectionConstructor ctor) {
		LOGGER.debug("Handling incoming UDP connection");
		final ServerManagerState stateSnapshot = getCurrentState(); //We are not synced here, but if it is STOPPING or STOPPED it can never be RUNNING again
		if(stateSnapshot.ordinal() > ServerManagerState.RUNNING.ordinal()) {
			LOGGER.warning("Declining incoming UDP socket connection because server is already %s", stateSnapshot);
			//Disconnect
			sendUdpLogout(remoteAddress);
			return;
		}
		
		
		final FilterRawConnectionEvent event1 = new FilterRawConnectionEvent(remoteAddress, 
				ManagerInstanceProvider.generateNetworkIdName("RemoteId-"));
		getEventDispatcher().post(FilterRawConnection, event1);
		if(event1.isCancelled()) {
			//Disconnect
			sendUdpLogout(remoteAddress);
		} else {
			final NetworkID networkId = NetworkID.createID(event1.getNetworkIdName(), remoteAddress);

			//Next event
			final ConfigureConnectionEvent event2 = new ConfigureConnectionEvent(this, networkId);
			getEventDispatcher().post(ConfigureConnection, event2);

			
			try {
				NetworkConnection udpConnection = ctor.construct(networkId, event2.getCustomObject());
				
				//This will start the sync. An exclusive lock for this whole method would be too expensive
				if(!addInitializedConnection(udpConnection)) {
					//Can't connect after all, maybe the server was stopped in the time we created the connection
					LOGGER.warning("Re-Closed an initialized connection: Server was stopped during connection init");
					udpConnection.closeConnection();
				}
			} catch (IOException e) {
				LOGGER.error("Connection moved to an invalid state while creating connection object", e);
			} 
		}
	}
	
	protected abstract void sendUdpLogout(SocketAddress remoteAddress);

}
