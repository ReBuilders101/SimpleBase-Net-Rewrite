package dev.lb.simplebase.net.packet.converter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import dev.lb.simplebase.net.NetworkManager;
import dev.lb.simplebase.net.io.ByteDataHelper;

/**
 * Optionally decompresses byte data
 */
public abstract class ByteInflater implements AutoCloseable {

	/**
	 * Deflate byte buffer contets.
	 * @param data The input data
	 * @return The processed data
	 * @throws IOException When the inflate alogrithm encounters a {@link DataFormatException}.
	 */
	public abstract ByteBuffer inflate(ByteBuffer data) throws IOException;
	@Override public abstract void close();
	
	/**
	 * Applies no decompression to the buffer.
	 */
	public static final ByteInflater NO_COMPRESSION = new ByteInflater() {
		
		@Override
		public ByteBuffer inflate(ByteBuffer data) {
			return data;
		}

		@Override
		public void close() {}
	};
	
	/**
	 * Uses the zip/deflate algorithm as implemented in the {@link Inflater} class.
	 * The uncompressed length of the data read from a 4-byte prefix.s
	 */
	public static final ByteInflater ZIP_COMPRESSION_PREFIXED = new ByteInflater() {
		
		{
			NetworkManager.addCleanupTask(this::close);
		}
		
		private final Inflater inf = new Inflater();
		
		@Override
		public ByteBuffer inflate(ByteBuffer data) throws IOException {
			inf.reset();
			int deflatedLength = ByteDataHelper.cInt(data);
			
			byte[] array = new byte[data.remaining()];
			byte[] deflated = new byte[deflatedLength];
			data.get(array);
			
			inf.setInput(array);
			
			try {
				int length = inf.inflate(deflated);
				if(length != deflatedLength) throw new IOException(
						"Inflate/Deflate mismatch: data should be " + deflatedLength + " bytes but was " + length + " bytes");
				return ByteBuffer.wrap(deflated);
			} catch (DataFormatException e) {
				throw new IOException(e);
			}
		}

		@Override
		public void close() {
			inf.end();
		}
	};
}
