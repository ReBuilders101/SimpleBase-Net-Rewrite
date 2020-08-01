package dev.lb.simplebase.net.connection;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;

public class InternalNetworkConnection extends NetworkConnection {

	private InternalNetworkConnection peer;
	
	public InternalNetworkConnection(NetworkManagerCommon networkManager, NetworkID remoteID,
			int checkTimeoutMS, boolean serverSide, Object customObject) {
		super(networkManager, remoteID, NetworkConnectionState.INITIALIZED, checkTimeoutMS, serverSide, customObject);
		this.peer = null;
	}
	
	public InternalNetworkConnection(NetworkManagerCommon networkManager,
			InternalNetworkConnection peer, int checkTimeoutMS, boolean serverSide, Object customObject) {
		super(networkManager, peer.getLocalID(), NetworkConnectionState.OPEN, checkTimeoutMS, serverSide, customObject);
		this.peer = peer;
	}

	private void setPeerConnection(InternalNetworkConnection connection) {
		//This is significant, hard fail here
		if(this.peer != null) throw new IllegalStateException("Cannot set peer connection twice");
		if(!connection.getLocalID().equals(this.getRemoteID()) || !connection.getRemoteID().equals(this.getLocalID()))
			throw new IllegalArgumentException("Peer connection must have mirrored remote/local IDs");
		this.peer = connection;
	}
	
	@Override
	protected NetworkConnectionState openConnectionImpl() {
		final InternalNetworkConnection foundPeer = NetworkManager.InternalAccess.INSTANCE.createInternalConnectionPeer(this);
		if(foundPeer == null) { //failed to find
			//Fail
			STATE_LOGGER.error("Failed to find a peer for server id %s", remoteID);
			return NetworkConnectionState.CLOSED;
		} else {
			this.setPeerConnection(foundPeer);
		}
		return NetworkConnectionState.OPEN;
	}

	@Override
	protected NetworkConnectionState closeConnectionImpl(ConnectionCloseReason reason) {
		 //If the peer has called this method, don't tell him again because he already knows
		if(reason != ConnectionCloseReason.REMOTE) {
			peer.closeConnection(ConnectionCloseReason.REMOTE);			
		}
		//Just remove from the server
		postEventAndRemoveConnection(reason, null);
		
		return NetworkConnectionState.CLOSED;
	}

	@Override
	protected boolean checkConnectionImpl(int uuid) {
		requirePeer();
		peer.receiveConnectionCheck(uuid);
		return true;
	}

	@Override
	protected void sendPacketImpl(Packet packet) {
		requirePeer();
		peer.receivePacket(packet);
	}

	@Override
	public void receiveConnectionCheck(int uuid) {
		requirePeer();
		synchronized (currentState) {
			//Reply only if still open
			if(currentState.canSendData())	peer.pingTracker.confirmPing(uuid);
		}
	}

	private void requirePeer() {
		STATE_LOGGER.fatal("Unexpected State: Peer not present (%s)", currentState);
		throw new IllegalStateException("Peer not present");
	}
	
}
