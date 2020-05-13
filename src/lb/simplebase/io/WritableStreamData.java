package lb.simplebase.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class WritableStreamData implements WritableByteData {

	private final ByteArrayOutputStream writeData;
	private byte[] readData;
	private boolean readValid;

	public WritableStreamData() {
		this.writeData = new ByteArrayOutputStream();
		this.readData = null;
		this.readValid = false;
	}
	
	
	private void updateRead() {
		if(!readValid) {
			readData = writeData.toByteArray();
		}
		readValid = true;
	}


	@Override
	public void writeByte(byte b) {
		writeData.write(b);
		readValid = false;
	}


	@Override
	public void write(byte[] data) {
		try {
			writeData.write(data);
		} catch (IOException e) {
			e.printStackTrace(); //This probably does not ever happen
		} finally {
			readValid = false;
		}
	}
	
	public byte[] getAsArray() {
		updateRead();
		return Arrays.copyOf(readData, readData.length);
	}
	
	public byte[] internalArray() {
		updateRead();
		return readData;
	}
	
	public int getLength() {
		return writeData.size();
	}
	
}
