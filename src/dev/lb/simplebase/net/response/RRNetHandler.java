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
import dev.lb.simplebase.net.task.ValueTask;
import dev.lb.simplebase.net.util.Pair;

public class RRNetHandler implements PacketHandler {
	static final Logger LOGGER = NetworkManager.getModuleLogger("packet-handler");
	
	//Use a HashMap to quickly lokkup uuids:
	private final HashMap<UUID, RequestDetails<?>> activeRequests;
	private final NetworkManagerCommon manager;
	private final PacketHandler defaultHandler;
	
	public RRNetHandler(NetworkManagerCommon manager, PacketHandler defaultHandler) {
		this.activeRequests = new HashMap<>();
		this.defaultHandler = defaultHandler;
		this.manager = manager;
	}
	
	public RRNetHandler(NetworkManagerCommon manager) {
		this(manager, PacketHandler.createEmpty());
	}
	
	public <ResponseType extends RRPacket> ValueTask.PairTask<ResponseType, PacketContext> sendPacket(RRPacket.Request<ResponseType> packet) {
		if(manager instanceof NetworkManagerClient) {
			return sendPacket(((NetworkManagerClient) manager).getServerID(), packet);
		} else {
			throw new UnsupportedOperationException("Sending without a destination ID is only possible for clients");
		}
	}
	
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
	
	public int getPendingReplyCount() {
		synchronized (activeRequests) {
			return activeRequests.size();
		}
	}
	
	public void cancelPendingReplies() {
		synchronized (activeRequests) {
			activeRequests.forEach((uuid, details) -> 
				details.taskCompleted.cancelled(new Exception("Task cancelled by user: Cleared pending tasks in RRNetHandler")));
			activeRequests.clear();
		}
	}
	
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
