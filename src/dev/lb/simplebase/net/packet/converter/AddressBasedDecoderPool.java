package dev.lb.simplebase.net.packet.converter;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import dev.lb.simplebase.net.manager.NetworkManagerProperties;

/**
 * Maintains a pool of {@link ByteAccumulator}s with exchangable decode targets
 * (Wrapped in a {@link MutableAddressConnectionAdapter}).
 */
public class AddressBasedDecoderPool {
	
	private final Function<InetSocketAddress, ? extends MutableAddressConnectionAdapter> factory;
	private final Set<ByteAccumulator> freeAdapters;
	private final Map<InetSocketAddress, ByteAccumulator> usedAdapters;
	private final Object mapLock;
	private final NetworkManagerProperties manager;
	
	/**
	 * Creates a new decoder pool.
	 * @param newAdapters A function that produces reuseable decode targets ({@link MutableAddressConnectionAdapter}s)
	 * initialized with a certain {@link InetSocketAddress}.
	 * @param manager A {@link NetworkManagerProperties} that provides context such as configs and converters
	 */
	public AddressBasedDecoderPool(Function<InetSocketAddress, ? extends MutableAddressConnectionAdapter> newAdapters, NetworkManagerProperties manager) {
		this.factory = newAdapters;
		this.mapLock = new Object();
		this.freeAdapters = new HashSet<>();
		this.usedAdapters = new HashMap<>();
		this.manager = manager;
	}
	
	/**
	 * Decode a received buffer from a source address.
	 * The decoded packet will be passed to an {@link ConnectionAdapter} produced by the factory method
	 * defined in the constructor
	 * @param source The source {@link InetSocketAddress} of the data
	 * @param data The data in a {@link ByteBuffer}
	 */	
	public void decode(InetSocketAddress source, ByteBuffer data) {
		final ByteAccumulator decoder = getAndMoveDecoder(source);
		decoder.acceptBytes(data);
		freeAndMoveDecoder(source);
	}
	
	private ByteAccumulator getAndMoveDecoder(InetSocketAddress address) {
		synchronized (mapLock) {
			if(usedAdapters.containsKey(address)) {
				final ByteAccumulator decoder = usedAdapters.get(address);
				getCounter(decoder).acquire();
				return decoder;
			} else {
				if(freeAdapters.size() > 0) {
					final ByteAccumulator decoder = freeAdapters.iterator().next();
					freeAdapters.remove(decoder);
					setAddress(decoder, address);
					getCounter(decoder).acquire();
					usedAdapters.put(address, decoder);
					return decoder;
				} else {
					final ByteAccumulator decoder = new ByteAccumulator(manager, factory.apply(address));
					getCounter(decoder).acquire();
					usedAdapters.put(address, decoder);
					return decoder;
				}
			}
		}
	}
	
	private void freeAndMoveDecoder(InetSocketAddress address) {
		synchronized (mapLock) {
			if(usedAdapters.containsKey(address)) {
				final ByteAccumulator decoder = usedAdapters.get(address);
				getCounter(decoder).release();
				//Only release a decoder when a packet is complete, otherwise it might get another data segment
				if(getCounter(decoder).getCounter() == 0 && decoder.isDone()) {
					usedAdapters.remove(address, decoder);
					decoder.resetToFindFormat(); //clear any extra data
					freeAdapters.add(decoder);
				}
			} else {
				throw new IllegalStateException("Cannot free an unmapped adapter");
			}
		}
	}
	
	private MutableAddressConnectionAdapter.ReferenceCounter getCounter(ByteAccumulator decoder) {
		return ((MutableAddressConnectionAdapter) decoder.getConnectionAdapter()).getUseCountManager();
	}
	
	private void setAddress(ByteAccumulator decoder, InetSocketAddress address) {
		((MutableAddressConnectionAdapter) decoder.getConnectionAdapter()).setAddress(address);
	}
	
}
