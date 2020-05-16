package lb.simplebase.net.io;

import dev.lb.simplebase.net.annotation.Internal;

/**
 * Internal interface for a {@link WritableByteData} that uses a backing byte array
 */
@Internal
public interface WritableArrayData extends WritableByteData {

	public byte[] getArray();
	
}
