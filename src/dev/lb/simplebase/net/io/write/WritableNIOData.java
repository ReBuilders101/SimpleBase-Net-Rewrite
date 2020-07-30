package dev.lb.simplebase.net.io.write;

import java.nio.ByteBuffer;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.io.WritableByteData;

/**
 * Internal interface for a {@link WritableByteData} that uses a backing byte buffer
 */
@Internal
public interface WritableNIOData extends WritableByteData {

	public ByteBuffer getBuffer();
	
}
