package dev.lb.simplebase.net.io.write;

import dev.lb.simplebase.net.annotation.Internal;
import dev.lb.simplebase.net.io.WritableByteData;

/**
 * Internal interface for a {@link WritableByteData} that uses a backing byte array
 */
@Internal
public interface WritableArrayData extends WritableByteData {

	public byte[] getArray();
	
}
