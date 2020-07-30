package lb.simplebase.net.io.read;

import java.nio.ByteBuffer;

import dev.lb.simplebase.net.annotation.Internal;
import lb.simplebase.net.io.ReadableByteData;

/**
 * Implementation of {@link ReadableByteData}, internally used to deserialize packets
 */
@Internal
public class NIOReadableData implements ReadableByteData {

	private final ByteBuffer data;
	
	public NIOReadableData(ByteBuffer data) {
		this.data = data;
	}
	
	@Override
	public byte readByte() {
		return data.get();
	}

	@Override
	public void skip(int amount) {
		data.position(data.position() + amount);
	}

	@Override
	public boolean canRead() {
		return data.remaining() > 0;
	}

	@Override
	public int getRemainingLength() {
		return data.remaining();
	}

}
