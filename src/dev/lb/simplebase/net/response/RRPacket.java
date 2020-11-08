package dev.lb.simplebase.net.response;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;

import dev.lb.simplebase.net.packet.Packet;

/**
 * A request or response packet that can be used in the {@link RRNetHandler}.
 * <p>
 * Must be subclassed by an actual packet implementation.<br>
 * Response packets should extend this class directly,
 * while request packet should extend the {@link Request} class instead.
 * </p>
 */
public abstract class RRPacket implements Packet {
	
	private UUID uuid;
	
	/**
	 * Creates a {@link RRPacket} with an existing {@link UUID}.
	 * Usually used to create a response packet with the same UUID as the request.
	 * @param existingUUID The {@link UUID} of the packet
	 */
	protected RRPacket(UUID existingUUID) {
		this.uuid = Objects.requireNonNull(existingUUID, "Cannot set packet UUID to null");
	}
	
	/**
	 * Creates a {@link RRPacket} with a randomly generated {@link UUID}. 
	 * Usually used to create a new request packet
	 * @param generateUUID If {@code true}, a random {@link UUID} will be created.
	 */
	protected RRPacket(boolean generateUUID) {
		this.uuid = generateUUID ? UUID.randomUUID() : null;
	}
	
	/**
	 * Creates a {@link RRPacket} with no assigned {@link UUID}.
	 * Usually used to construct a packet for decoding that will be initialized later.
	 */
	protected RRPacket() {
		this.uuid = null;
	}
	
	/**
	 * The {@link UUID} for this request or response packet.
	 * @return The {@link UUID} for this packet
	 */
	public UUID getUUID() {
		return uuid;
	}
	
	/**
	 * Can be used to set the packet UUID in the {@link #readData(dev.lb.simplebase.net.io.ReadableByteData)}
	 * method. The UUID can only be set once (this includes setting in the constructor) and cannot be
	 * set to {@code null}
	 * @param uuid The {@link UUID} for this packet
	 */
	protected void setUUID(UUID uuid) {
		if(this.uuid == null) {
			this.uuid = Objects.requireNonNull(uuid, "Cannot set packet UUID to null");
		} else {
			throw new IllegalStateException("UUID can only be set once");
		}
	}
	
	/**
	 * A request packet that can be sent with the {@link RRNetHandler}.
	 * @param <ResponseType> The type of the response packet
	 */
	public static abstract class Request<ResponseType extends RRPacket> extends RRPacket {
		
		protected Request(boolean generateUUID) {
			super(generateUUID);
		}

		/**
		 * The {@link Class} of the response packet expected by this request.
		 * <p>
		 * The default implementation attempts to infer this class from the
		 * declaration of the extanding class, if possible.<br>
		 * The type can be inferred if the generic type is declared in the extanding
		 * class definition (e.g. <pre>{@code class TestRequestPacket extends RRPacket.Request<TestResponsePacket>}</pre>).<br>
		 * The type cannot be inferred if the generic type is not declared directly
		 * (e.g. <pre>{@code class AbstractRequestPacket<T extends AbstractResponsePacket> extends RRPacket.Request<T>}</pre>).
		 * </p><p>
		 * If the type cannot be inferred by reflection, this default implementation will throw
		 * an {@link UnsupportedOperationException} and has to be reimplemented by the extending class.
		 * </p>
		 * @return The {@link Class} of the expected response packet.
		 */
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
