package dev.lb.simplebase.net.connection;

import dev.lb.simplebase.net.InternalServerProvider;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.task.Task;

@Internal
public class InternalNetworkConnection extends NetworkConnection {

	private InternalNetworkConnection peer;
	
	public InternalNetworkConnection(NetworkManagerCommon networkManager, NetworkID remoteID, Object customObject) {
		super(networkManager, remoteID, NetworkConnectionState.INITIALIZED,
				networkManager.getConfig().getConnectionCheckTimeout(), false, customObject);
		this.peer = null;
	}
	
	public InternalNetworkConnection(NetworkManagerCommon networkManager, InternalNetworkConnection peer, Object customObject) {
		super(networkManager, peer.getLocalID(), NetworkConnectionState.OPEN, 
				networkManager.getConfig().getConnectionCheckTimeout(), true, customObject);
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
	protected Task openConnectionImpl() {
		final InternalNetworkConnection foundPeer = InternalServerProvider.createInternalConnectionPeer(this);
		if(foundPeer == null) { //failed to find
			//Fail
			STATE_LOGGER.error("Failed to find a peer for server id %s", remoteID);
			currentState = NetworkConnectionState.CLOSED;
		} else {
			this.setPeerConnection(foundPeer);
		}
		currentState = NetworkConnectionState.OPEN;
		return Task.completed();
	}

	@Override
	protected Task closeConnectionImpl(ConnectionCloseReason reason) {
		 //If the peer has called this method, don't tell him again because he already knows
		if(reason != ConnectionCloseReason.REMOTE) {
			peer.closeConnection(ConnectionCloseReason.REMOTE);			
		}
		//Just remove from the server
		postEventAndRemoveConnection(reason, null);
		currentState = NetworkConnectionState.CLOSED;
		return Task.completed();
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
		peer.receivePacket(packet.copy());
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
		if(peer == null) {
			STATE_LOGGER.fatal("Unexpected State: Peer not present (%s)", currentState);
			throw new IllegalStateException("Peer not present");
		}
	}
	
}
