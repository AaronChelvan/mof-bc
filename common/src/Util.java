import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

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
	
	// Given a socket, return the name of the client connected to that socket
	public static String socketClientName(Socket s) throws UnknownHostException {
		// IP addresses of the nodes in the network
		String node1IP = InetAddress.getByName("node1").getHostAddress();
		String node2IP = InetAddress.getByName("node2").getHostAddress();
		String miner1IP = InetAddress.getByName("miner1").getHostAddress();
		System.out.println(miner1IP);
		
		// Get the IP address of the client
		String clientIP = s.getRemoteSocketAddress().toString();
		clientIP = clientIP.substring(1); // Strip the leading "/"
		clientIP = clientIP.split(":")[0]; // Ignore the port. We only want the IP address.
		
		if (clientIP.equals(node1IP)) {
			return "node1";
		} else if (clientIP.equals(node2IP)) {
			return "node2";
		} else if (clientIP.equals(miner1IP)){
			return "miner1";
		} else {
			return "";
		}
	}

}
