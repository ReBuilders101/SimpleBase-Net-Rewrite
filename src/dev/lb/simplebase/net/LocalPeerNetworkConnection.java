package dev.lb.simplebase.net;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;

@Internal
class LocalPeerNetworkConnection extends NetworkConnection{

	private LocalPeerNetworkConnection peerConnection;
	private PacketReceiverThread receiverThread;
	
	protected LocalPeerNetworkConnection(NetworkID localID, NetworkID remoteID, NetworkManagerCommon networkManager,
			int checkTimeoutMS, boolean serverSide, Object customObject) {
		super(localID, remoteID, networkManager, NetworkConnectionState.INITIALIZED, checkTimeoutMS, serverSide, customObject);
		this.peerConnection = null;
		this.receiverThread = null;
	}
	
	protected LocalPeerNetworkConnection(NetworkID localID, NetworkID remoteID, NetworkManagerCommon networkManager,
			int checkTimeoutMS, boolean serverSide, Object customObject, LocalPeerNetworkConnection peerConnection) {
		super(localID, remoteID, networkManager, NetworkConnectionState.OPENING, checkTimeoutMS, serverSide, customObject);
		//If we have a peer, start as opening
		this.peerConnection = peerConnection;
		startReceiverThread();
		currentState = NetworkConnectionState.OPEN;
	}

	private void startReceiverThread() {
		NetworkManager.NET_LOG.info("Starting packet receiver thread for connection from %s to %s", getLocalID(), getRemoteID());
		this.receiverThread = new PacketReceiverThread();
		this.receiverThread.start();
	}
	
	@Override
	protected void openConnectionImpl() {
		currentState = NetworkConnectionState.OPENING;
		//We are already locked and checked for state now, but one more check can't hurt
		if(peerConnection != null) throw new IllegalStateException("Connection to open already has a peer");
		final LocalPeerNetworkConnection peer = InternalServerManager.createServerPeer(this);
		if(peer == null) {
			NetworkManager.NET_LOG.warning("Could not find local server peer for " + getRemoteID().toString(true));
			currentState = NetworkConnectionState.CLOSED;
		} else {
			peerConnection = peer;
			//Start the receiver thread
			startReceiverThread();
			currentState = NetworkConnectionState.OPEN;
		}
		
	}
	
	/**
	 * Receives a packet from the peer.
	 * @param packet The packet
	 */
	protected boolean receiveInternalOnThread(Packet packet) {
		return receiverThread.handlePacket(packet);//Don't need that yet
	}
	
	
	@Override
	protected void closeConnectionImpl(ConnectionCloseReason reason) {
		closeConnectionImpl(reason, null, true);
	}
	
	protected void closeConnectionImpl(ConnectionCloseReason reason, Exception exception, boolean notifyPeer) {
		currentState = NetworkConnectionState.CLOSING;
		NetworkManager.NET_LOG.info("Closing connection: %s; Reason: %s", getClass().getCanonicalName(), reason);
		if(notifyPeer) peerConnection.receivePeerCloseRequest(); //Close the other side too, if requested
		postEventAndRemoveConnection(reason, exception);
		receiverThread.interrupt();
		currentState = NetworkConnectionState.CLOSED;	
	}

	@Override
	protected void checkConnectionImpl(int uuid) {
		peerConnection.receiveConnectionCheck(uuid);
	}

	@Override
	protected void sendPacketImpl(Packet packet) {
		if(!peerConnection.receiveInternalOnThread(packet)) {
			//If it couldn't be sent:
			//No regular way to process this, as it is not usual to get this info on normal net connections
			NetworkManager.NET_LOG.warning("Could not transmit internal packet: handler thread queue full");
		}
	}

	@Override
	protected void receiveConnectionCheck(int uuid) {
		peerConnection.receiveConnectionCheckReply(uuid);
	}
	
	protected void receivePeerCloseRequest() {
		synchronized (lockCurrentState) {
			closeConnectionImpl(ConnectionCloseReason.REMOTE, null, false);
		}
	}

	@Override
	protected void receiveUDPLogout() {
		synchronized (lockCurrentState) {
			NetworkManager.NET_LOG.warning("Local Peer connections should never receive UDP logout requests");
			closeConnectionImpl(ConnectionCloseReason.REMOTE);
		}
	}
	
	@Override
	protected void closeTimeoutImpl() {
		synchronized (lockCurrentState) {
			closeConnectionImpl(ConnectionCloseReason.TIMEOUT);
		}
	}
	
	private class PacketReceiverThread extends Thread {
		
		private final BlockingQueue<Packet> packetList;
		
		private PacketReceiverThread() {
			packetList = new LinkedBlockingQueue<>();
		}
		
		private boolean handlePacket(Packet packet) {
			return packetList.offer(packet);
		}

		@Override
		public void run() {
			while(!Thread.interrupted()) {
				try {
					final Packet current = packetList.take();
					LocalPeerNetworkConnection.this.receivePacket(current);
				} catch (InterruptedException e) {
					//Restore flag and exit
					Thread.currentThread().interrupt();
				}
			}
			NetworkManager.NET_LOG.info("Packet receiver thread for connection form %s to &s terminated", getLocalID(), getRemoteID());
		}
		
	}
	
}
