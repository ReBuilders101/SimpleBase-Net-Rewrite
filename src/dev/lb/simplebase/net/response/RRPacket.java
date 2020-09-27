package dev.lb.simplebase.net.response;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;

import dev.lb.simplebase.net.packet.Packet;

public abstract class RRPacket implements Packet {
	
	private UUID uuid;
	
	protected RRPacket(UUID existingUUID) {
		this.uuid = Objects.requireNonNull(uuid, "Cannot set packet UUID to null");
	}
	
	protected RRPacket(boolean generateUUID) {
		this.uuid = generateUUID ? UUID.randomUUID() : null;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	protected void setUUID(UUID uuid) {
		if(this.uuid == null) {
			this.uuid = Objects.requireNonNull(uuid, "Cannot set packet UUID to null");
		} else {
			throw new IllegalStateException("UUID can only be set once");
		}
	}
	
	public static abstract class Request<ResponseType extends RRPacket> extends RRPacket {
		
		protected Request(boolean generateUUID) {
			super(generateUUID);
		}

		@SuppressWarnings("unchecked")
		public Class<ResponseType> getResponsePacketClass() {
			final Type superclass = getClass().getGenericSuperclass();
			if(superclass instanceof ParameterizedType) {
				final ParameterizedType p = (ParameterizedType) superclass;
				final Type raw = p.getRawType();
				if(raw instanceof Class && (Class<?>) raw == RRPacket.Request.class) {
					final Type generic = p.getActualTypeArguments()[0];
					if(generic instanceof Class) return (Class<ResponseType>) generic;
				}
			}
			throw new UnsupportedOperationException("Cannot automatically find generic type: Implement manually");
		}
	}
}
