package dev.lb.simplebase.net.packet.converter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import dev.lb.simplebase.net.io.ByteDataHelper;

public abstract class ByteInflater implements AutoCloseable {

	public abstract ByteBuffer inflate(ByteBuffer data) throws IOException;
	@Override public abstract void close();
	
	public static final ByteInflater NO_COMPRESSION = new ByteInflater() {
		
		@Override
		public ByteBuffer inflate(ByteBuffer data) {
			return data;
		}

		@Override
		public void close() {}
	};
	
	public static final ByteInflater ZIP_COMPRESSION_PREFIXED = new ByteInflater() {
		
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
