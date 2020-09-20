package dev.lb.simplebase.net.packet.converter;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import dev.lb.simplebase.net.packet.PacketIDMappingProvider;

public class AddressBasedDecoderPool {
	
	private final Function<InetSocketAddress, ? extends MutableAddressConnectionAdapter> factory;
	private final PacketIDMappingProvider mappings;
	private final Set<ByteAccumulator> freeAdapters;
	private final Map<InetSocketAddress, ByteAccumulator> usedAdapters;
	private final Object mapLock;
	private final int decoderBufferSize;
	
	public AddressBasedDecoderPool(Function<InetSocketAddress, ? extends MutableAddressConnectionAdapter> newAdapters, PacketIDMappingProvider mappings, int bufferSize) {
		this.factory = newAdapters;
		this.mappings = mappings;
		this.mapLock = new Object();
		this.freeAdapters = new HashSet<>();
		this.usedAdapters = new HashMap<>();
		this.decoderBufferSize = bufferSize;
	}
	
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
					final ByteAccumulator decoder = new ByteAccumulator(factory.apply(address), mappings, decoderBufferSize);
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
				if(getCounter(decoder).getCounter() == 0) {
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
