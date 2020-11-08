package dev.lb.simplebase.net.response;

import java.util.HashMap;
import java.util.UUID;
import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.Logger;
import dev.lb.simplebase.net.manager.NetworkManagerClient;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.packet.handler.PacketHandler;
import dev.lb.simplebase.net.response.RRPacket.Request;
import dev.lb.simplebase.net.task.ValueTask;
import dev.lb.simplebase.net.util.Pair;

/**
 * A {@link RRNetHandler} can provide support for a simple request/response packet model.
 */
public class RRNetHandler implements PacketHandler {
	static final Logger LOGGER = NetworkManager.getModuleLogger("packet-handler");
	
	//Use a HashMap to quickly lokkup uuids:
	private final HashMap<UUID, RequestDetails<?>> activeRequests;
	private final NetworkManagerCommon manager;
	private final PacketHandler defaultHandler;
	
	/**
	 * Creates a new {@link RRNetHandler} for a network manager and a default handler.
	 * @param manager The {@link NetworkManagerCommon} used to send requests
	 * @param defaultHandler The {@link PacketHandler} that will handle all packets that are not a subtype of {@link RRPacket}
	 */
	public RRNetHandler(NetworkManagerCommon manager, PacketHandler defaultHandler) {
		this.activeRequests = new HashMap<>();
		this.defaultHandler = defaultHandler;
		this.manager = manager;
	}
	
	/**
	 * Creates a new {@link RRNetHandler} for a network manager and an empty default handler.
	 * @param manager The {@link NetworkManagerCommon} used to send requests
	 */
	public RRNetHandler(NetworkManagerCommon manager) {
		this(manager, PacketHandler.createEmpty());
	}
	
	/**
	 * Sends a request packet from a client to the connected server.
	 * <p>
	 * This method will only work when the manager used in the constructor is an instance of {@link NetworkManagerClient}.
	 * </p>
	 * @param <ResponseType> The type of response packet for this request
	 * @param packet The {@link Request} packet to send
	 * @return A {@link ValueTask} that will complete once the response has been received.
	 * Both response packet and a {@link PacketContext} are included with the task
	 * @throws UnsupportedOperationException When the manager is not an instance of {@link NetworkManagerClient}
	 */
	public <ResponseType extends RRPacket> ValueTask.PairTask<ResponseType, PacketContext> sendPacket(RRPacket.Request<ResponseType> packet) {
		if(manager instanceof NetworkManagerClient) {
			return sendPacket(((NetworkManagerClient) manager).getServerID(), packet);
		} else {
			throw new UnsupportedOperationException("Sending without a destination ID is only possible for clients");
		}
	}
	
	/**
	 * Sends a request packet from a client to the connected server.
	 * @param <ResponseType> The type of response packet for this request
	 * @param target The destination {@link NetworkID} for the packet
	 * @param packet The {@link Request} packet to send
	 * @return A {@link ValueTask} that will complete once the response has been received.
	 * If the packet could not be sent, the returned task will be cancelled
	 * Both response packet and a {@link PacketContext} are included with the task
	 */
	public <ResponseType extends RRPacket> ValueTask.PairTask<ResponseType, PacketContext> sendPacket(NetworkID target, RRPacket.Request<ResponseType> packet) {
		final RequestDetails<ResponseType> details = new RequestDetails<>(packet.getUUID(), target, packet.getResponsePacketClass());
		
		synchronized (activeRequests) {
			if(activeRequests.containsKey(details.getUUID())) {
				throw new RuntimeException("Duplicate request UUID");
			} else {
				final boolean sent = manager.sendPacketTo(target, packet);
				
				if(sent) {
					activeRequests.put(details.getUUID(), details);
					return details.getTask();
				} else {
					LOGGER.warning("Cannot send packet to " + target);
					final Pair<ResponseType, PacketContext> dummy = null; //Prevents unchecked cast
					return ValueTask.ofPair(ValueTask.cancelled(new Exception("Packet was not sent (see log)"), dummy));
				}
			}
		}
	}
	
	@Override
	public void handlePacket(Packet packet, PacketContext context) {
		if(packet instanceof RRPacket) {
			final RRPacket rrp = (RRPacket) packet;
			final RequestDetails<?> req = activeRequests.get(rrp.getUUID());
			if(req == null) {
				LOGGER.warning("Received RR response packet without a corresponding request UUID");
			} else if(req.getRemoteID() != context.getRemoteID()) {
				LOGGER.warning("Received RR response packet from a different remote than the request was sent to");
			} else if(req.getResponseClass() != rrp.getClass()) {
				LOGGER.warning("Received RR response packet with a different type than the request asked for");
			} else {
				synchronized (activeRequests) {
					final RequestDetails<?> syncReq = activeRequests.get(rrp.getUUID());
					if(syncReq == null) {
						LOGGER.warning("Received RR response packet without a corresponding request UUID (missed sync)");
					} else if(syncReq == req) {
						req.handle(rrp, context);
						activeRequests.remove(rrp.getUUID(), req);
					} else {
						LOGGER.error("Received RR response packet: missed sync - request was altered");
					}
				}
			}
		} else {
			defaultHandler.handlePacket(packet, context);
		}
	}
	
	/**
	 * The amount of sent request packets that have not received a reply packet yet.
	 * @return The amount of pending replies
	 */
	public int getPendingReplyCount() {
		synchronized (activeRequests) {
			return activeRequests.size();
		}
	}
	
	/**
	 * Removes all request packets that have not received a response yet from the internal list.
	 * If a response is received after the request has been removed, it will be discarded.
	 */
	public void cancelPendingReplies() {
		synchronized (activeRequests) {
			activeRequests.forEach((uuid, details) -> 
				details.taskCompleted.cancelled(new Exception("Task cancelled by user: Cleared pending tasks in RRNetHandler")));
			activeRequests.clear();
		}
	}
	
	/**
	 * The amount of sent request packets to a certain {@link NetworkID} that have not received a reply packet yet.
	 * @param forRemote The {@link NetworkID} that the request packets have been sent to
	 * @return The amount of pending replies
	 */
	public int getPendingReplyCount(NetworkID forRemote) {
		synchronized (activeRequests) {
			return (int) activeRequests.values().stream().filter((details) -> details.remote == forRemote).count();
		}
	}
	
	private static final class RequestDetails<RT extends RRPacket> {
		
		private final ValueTask.CompletionSource<Pair<RT, PacketContext>> taskCompleted;
		private final ValueTask.PairTask<RT, PacketContext> delegate;
		private final UUID uuid;
		private final NetworkID remote;
		private final Class<RT> responseType;
		
		private RequestDetails(UUID uuid, NetworkID remote, Class<RT> responseType) {
			final Pair<ValueTask<Pair<RT, PacketContext>>, ValueTask.CompletionSource<Pair<RT, PacketContext>>> oof = ValueTask.completable();
			this.taskCompleted = oof.getRight();
			this.delegate = ValueTask.ofPair(oof.getLeft());
			this.uuid = uuid;
			this.remote = remote;
			this.responseType = responseType;
		}
		
		public ValueTask.PairTask<RT, PacketContext> getTask() {
			return delegate;
		}
		
		public UUID getUUID() {
			return uuid;
		}
		
		public NetworkID getRemoteID() {
			return remote;
		}
		
		public Class<? extends RRPacket> getResponseClass() {
			return responseType;
		}
		
		@SuppressWarnings("unchecked")
		public void handle(RRPacket packet, PacketContext context) {
			if(taskCompleted.isSet()) throw new IllegalStateException("Already used this RequestDetails");
			taskCompleted.success(new Pair<>((RT) packet, context));
		}
	}
}
