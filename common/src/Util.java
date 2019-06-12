import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class Util {
	
	// Convert a Block object into a byte[] for storage in the DB
	// https://stackoverflow.com/a/3736247
	public static byte[] blockToBytes(Block b) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);
		out.writeObject(b);
		return bos.toByteArray();
	}
	
	// Convert a byte[] to a Block object
	public static Block bytesToBlock(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInputStream in = new ObjectInputStream(bis);
		return (Block) in.readObject();
	}

}
