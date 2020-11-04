package dev.lb.simplebase.net.io;

import java.nio.ByteBuffer;

/**
 * Interface for {@link WritableByteData} implementations that are backed by a NIO {@link ByteBuffer}.
 */
public interface WritableNIOData extends WritableByteData {

	/**
	 * The current backing buffer used by write operations
	 * @return The backing buffer
	 */
	public ByteBuffer getBuffer();
	
}
