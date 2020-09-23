package dev.lb.simplebase.net.packet.converter;

import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import dev.lb.simplebase.net.io.ByteDataHelper;

/**
 * Optionally compresses byte data
 */
public abstract class ByteDeflater implements AutoCloseable {

	public abstract ByteBuffer deflate(ByteBuffer data);
	@Override public abstract void close();
	
	public static final ByteDeflater NO_COMPRESSION = new ByteDeflater() {
		@Override
		public ByteBuffer deflate(ByteBuffer data) {
			return data;
		}

		@Override
		public void close() {}
	};
	
	public static final ByteDeflater ZIP_COMPRESSION_PREFIXED = new ByteDeflater() {
		
		private final Deflater def = new Deflater();
		
		@Override
		public ByteBuffer deflate(ByteBuffer data) {
			def.reset();
			
			int uncompressedSize = data.remaining();
			byte[] dataArray = new byte[uncompressedSize];
			data.get(dataArray);
			
			
			def.setInput(dataArray);
			def.finish();
			
			byte[] compressedData = null;
			int compressedSize = 4;
			
			//4 bytes to prefix the length later
			while(!def.needsInput()) {
				compressedData = expand(compressedData, compressedSize, uncompressedSize);
				int lastCompressed = def.deflate(compressedData, compressedSize, compressedData.length - compressedSize);
				compressedSize += lastCompressed;
			}
			
			//Write the prefix
			ByteDataHelper.cInt(uncompressedSize, compressedData, 0);
			
			return ByteBuffer.wrap(compressedData);
		}

		private byte[] expand(byte[] data, int alreadyUsed, int increment) {
			if(data == null) return new byte[increment];
			
			if(data.length <= alreadyUsed) {
				return data;
			} else {
				byte[] newData = new byte[alreadyUsed + increment];
				System.arraycopy(data, 0, newData, 0, data.length);
				return newData;
			}
		}
		
		@Override
		public void close() {
			def.end();
		}
	};
	
	
}