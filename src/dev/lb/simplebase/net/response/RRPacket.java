package dev.lb.simplebase.net.response;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.UUID;

import dev.lb.simplebase.net.packet.Packet;

public interface RRPacket extends Packet{
	
	public UUID getUUID();
	
	public static interface Request<ResponseType extends RRPacket> extends RRPacket {
		
		@SuppressWarnings("unchecked")
		public default Class<ResponseType> getResponsePacketClass() {
			final Type[] interfaces = getClass().getGenericInterfaces();
			for(Type i : interfaces) {
				if(i instanceof ParameterizedType) {
					final ParameterizedType p = (ParameterizedType) i;
					final Type raw = p.getRawType();
					if(raw instanceof Class && (Class<?>) raw == RRPacket.Request.class) {
						final Type generic = p.getActualTypeArguments()[0];
						if(generic instanceof Class) return (Class<ResponseType>) generic;
					}
				}
			}
			throw new UnsupportedOperationException("Cannot automatically find generic type: Implement manually");
		}
	}
}
