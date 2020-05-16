package lb.simplebase.net.io;

import java.nio.ByteBuffer;

import dev.lb.simplebase.net.annotation.Internal;

/**
 * Internal interface for a {@link WritableByteData} that uses a backing byte buffer
 */
@Internal
public interface WritableNIOData extends WritableByteData {

	public ByteBuffer getBuffer();
	
}
