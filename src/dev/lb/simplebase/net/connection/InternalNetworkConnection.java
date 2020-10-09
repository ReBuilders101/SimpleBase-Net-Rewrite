package dev.lb.simplebase.net.connection;

import java.util.Objects;

import dev.lb.simplebase.net.InternalServerProvider;
import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.events.ConnectionCloseReason;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.id.NetworkIDFunction;
import dev.lb.simplebase.net.manager.NetworkManagerClient;
import dev.lb.simplebase.net.manager.NetworkManagerServer;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.task.Task;
import dev.lb.simplebase.net.util.InternalAccess;

/**
 * A {@link NetworkConnection} implementation that connects a client and a server within the same application.
 * <p>
 * An {@link InternalNetworkConnection} does not encode/decode packets to/from bytes.
 * </p>
 */
@Internal
public class InternalNetworkConnection extends NetworkConnection {

	private InternalNetworkConnection peer;
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and can not be called directly.
	 * </p><hr><p>
	 * Creates an unconnected client-side {@link InternalNetworkConnection}.
	 * </p>
	 * @param networkManager The {@link NetworkManagerClient} that will hold this connection
	 * @param remoteID The {@link NetworkID} of the remote connection side, must implement {@link NetworkIDFunction#INTERNAL}
	 * @param customObject The object associated with this connection's {@link PacketContext}, may be {@code null}
	 * @throws NullPointerException When {@code networkManager} or {@code remoteID} are {@code null}
	 */
	@Internal
	public InternalNetworkConnection(NetworkManagerClient networkManager, NetworkID remoteID, Object customObject) {
		super(networkManager, remoteID, NetworkConnectionState.INITIALIZED,
				networkManager.getConfig().getConnectionCheckTimeout(), false, customObject);
		InternalAccess.assertCaller(NetworkManagerClient.class, 0, "Cannot instantiate InternalNetworkConnection directly");
		
		this.peer = null;
	}
	
	/**
	 * <h2>Internal use only</h2>
	 * <p>
	 * This method is used internally by the API and can not be called directly.
	 * </p><hr><p>
	 * Creates a connected server-side {@link InternalNetworkConnection}.
	 * </p>
	 * @param networkManager The {@link NetworkManagerServer} that will hold this connection
	 * @param peer The {@link InternalNetworkConnection} that represents the remote (client) side of the connection
	 * @param customObject The object associated with this connection's {@link PacketContext}, may be {@code null}
	 * @throws NullPointerException When {@code networkManager} or {@code peer} are {@code null}
	 */
	@Internal
	public InternalNetworkConnection(NetworkManagerServer networkManager, InternalNetworkConnection peer, Object customObject) {
		super(networkManager,  Objects.requireNonNull(peer, "'peer' parameter must not be null").getLocalID(),
				NetworkConnectionState.OPEN, networkManager.getConfig().getConnectionCheckTimeout(), true, customObject);
		InternalAccess.assertCaller(InternalServerProvider.class, 0, "Cannot instantiate InternalNetworkConnection directly");
		
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
