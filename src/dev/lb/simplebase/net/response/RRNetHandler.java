package dev.lb.simplebase.net.response;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.id.NetworkID;
import dev.lb.simplebase.net.log.AbstractLogger;
import dev.lb.simplebase.net.manager.NetworkManagerCommon;
import dev.lb.simplebase.net.packet.Packet;
import dev.lb.simplebase.net.packet.PacketContext;
import dev.lb.simplebase.net.packet.handler.PacketHandler;
import dev.lb.simplebase.net.util.AwaitableTask;
import dev.lb.simplebase.net.util.Task;

public class RRNetHandler implements PacketHandler {
	static final AbstractLogger LOGGER = NetworkManager.getModuleLogger("packet-handler");
	
	//Use a HashMap to quickly lokkup uuids:
	private final HashMap<UUID, RequestDetails> activeRequests;
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
	
	public <ResponseType extends RRPacket> Task sendPacket(NetworkID target,RRPacket.Request<ResponseType> packet,
			Consumer<ResponseType> handler) {
		return sendPacket(target, packet, handler, false);
	}
	
	@SuppressWarnings("unchecked")
	public <ResponseType extends RRPacket> Task sendPacket(NetworkID target,RRPacket.Request<ResponseType> packet,
			Consumer<ResponseType> handler,  boolean async) {
		final RequestDetails details = new RequestDetails(packet.getUUID(), target, packet.getResponsePacketClass(), (Consumer<RRPacket>) handler, async);
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
					return Task.completed();
				}
			}
		}
	}
	
	@Override
	public void handlePacket(Packet packet, PacketContext context) {
		if(packet instanceof RRPacket) {
			final RRPacket rrp = (RRPacket) packet;
			final RequestDetails req = activeRequests.get(rrp.getUUID());
			if(req == null) {
				LOGGER.warning("Received RR response packet without a corresponding request UUID");
			} else if(req.getRemoteID() != context.getRemoteID()) {
				LOGGER.warning("Received RR response packet from a different remote than the request was sent to");
			} else if(req.getResponseClass() != rrp.getClass()) {
				LOGGER.warning("Received RR response packet with a different type than the request asked for");
			} else {
				synchronized (activeRequests) {
					final RequestDetails syncReq = activeRequests.get(rrp.getUUID());
					if(syncReq == null) {
						LOGGER.warning("Received RR response packet without a corresponding request UUID (missed sync)");
					} else if(syncReq == req) {
						req.handle(rrp);
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
	
	private static final class RequestDetails {
		
		private final AwaitableTask task;
		private final UUID uuid;
		private final NetworkID remote;
		private final Class<? extends RRPacket> responseType;
		private final Consumer<RRPacket> handler;
		private final boolean async;
		
		private RequestDetails(UUID uuid, NetworkID remote, Class<? extends RRPacket> responseType, Consumer<RRPacket> handler, boolean async) {
			this.task = new AwaitableTask();
			this.uuid = uuid;
			this.remote = remote;
			this.responseType = responseType;
			this.handler = handler;
			this.async = async;
		}
		
		public Task getTask() {
			return task;
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
		
		public void handle(RRPacket packet) {
			if(task.isDone()) throw new IllegalStateException("Already used this RequestDetails");
			
			if(async) {
				CompletableFuture.runAsync(() -> handler.accept(packet));
			} else {
				handler.accept(packet);
			}
			
			task.release();
		}
	}
}
