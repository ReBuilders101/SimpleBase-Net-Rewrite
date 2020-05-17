package dev.lb.simplebase.net;

import java.util.concurrent.atomic.AtomicReference;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.events.ConnectionClosedEvent;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;

@Internal
class LocalPeerNetworkConnection extends NetworkConnection implements PacketHandler {

	private LocalPeerNetworkConnection peerConnection;
	private PacketThreadReceiver receiverThread;
	
	private final AtomicReference<PacketHandler> handler;
	
	protected LocalPeerNetworkConnection(NetworkID localID, NetworkID remoteID, NetworkManagerCommon networkManager,
			int checkTimeoutMS, boolean serverSide, Object customObject) {
		super(localID, remoteID, networkManager, NetworkConnectionState.INITIALIZED, checkTimeoutMS, serverSide, customObject);
		this.peerConnection = null;
		this.receiverThread = null;
		this.handler = new AtomicReference<>(this);
	}
	
	protected LocalPeerNetworkConnection(NetworkID localID, NetworkID remoteID, NetworkManagerCommon networkManager,
			int checkTimeoutMS, boolean serverSide, Object customObject, LocalPeerNetworkConnection peerConnection) {
		super(localID, remoteID, networkManager, NetworkConnectionState.OPENING, checkTimeoutMS, serverSide, customObject);
		//If we have a peer, start as opening
		this.peerConnection = peerConnection;
		this.handler = new AtomicReference<>(this);
		startReceiverThread();
		currentState = NetworkConnectionState.OPEN;
	}

	private void startReceiverThread() {
		this.receiverThread = new PacketThreadReceiver(handler, getNetworkManager().getEventDispatcher(), getNetworkManager().PacketReceiveRejected);
	}
	
	@Override
	protected void openConnectionImpl() {
		//We are already locked and checked for state now, but one more check can't hurt
		if(peerConnection != null) throw new IllegalStateException("Connection to open already has a peer");
		final LocalPeerNetworkConnection peer = LocalServerManager.createServerPeer(getRemoteID());
		if(peer == null) {
			NetworkManager.NET_LOG.warning("Could not find local server peer for " + getRemoteID().toString(true));
			currentState = NetworkConnectionState.CLOSED;
		} else {
			peerConnection = peer;
			currentState = NetworkConnectionState.OPENING;
			//Start the receiver thread
			startReceiverThread();
			currentState = NetworkConnectionState.OPEN;
		}
		
	}
	
	/**
	 * Receives a packet from the peer.
	 * @param packet The packet
	 */
	protected void receiveInternalOnThread(Packet packet) {
		receiverThread.handlePacket(packet, null);//Don't need that yet
	}
	
	@Override
	protected void closeConnectionImpl() {
		currentState = NetworkConnectionState.CLOSING;
		peerConnection.receivePeerCloseRequest(); //Close the other side too
		postCloseEvent(ConnectionCloseReason.EXPECTED, null);
	}

	@Override
	protected void checkConnectionImpl(int uuid) {
		peerConnection.receiveConnectionCheck(uuid);
	}

	@Override
	protected void sendPacketImpl(Packet packet) {
		peerConnection.receiveInternalOnThread(packet);
	}

	@Override
	protected void receiveConnectionCheck(int uuid) {
		peerConnection.receiveConnectionCheckReply(uuid);
	}
	
	protected void receivePeerCloseRequest() {
		synchronized (lockCurrentState) {
			currentState = NetworkConnectionState.CLOSING;
			postCloseEvent(ConnectionCloseReason.REMOTE, null);
		}
	}

	@Override
	protected void receiveUDPLogout() {
		synchronized (lockCurrentState) {
			currentState = NetworkConnectionState.CLOSING;
			postCloseEvent(ConnectionCloseReason.REMOTE, null);
		}
	}

	private void postCloseEvent(ConnectionCloseReason reason, Exception exception) {
		getNetworkManager().getEventDispatcher().post(getNetworkManager().ConnectionClosed,
				new ConnectionClosedEvent(reason, exception));
		getNetworkManager().removeConnectionWhileClosing(this);
		peerConnection = null;
		receiverThread.getOutputThread().interrupt();
		receiverThread = null;
		currentState = NetworkConnectionState.CLOSED;	
	}
	
	@Override
	public void handlePacket(Packet packet, PacketContext context) {
		receivePacket(packet);
	}

	@Override
	protected void closeTimeoutImpl() {
		synchronized (lockCurrentState) {
			currentState = NetworkConnectionState.CLOSING;
			postCloseEvent(ConnectionCloseReason.TIMEOUT, null);
		}
	}
	
}
