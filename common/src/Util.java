import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Util {
	
	// Convert a Block object into a byte[] for storage in the DB
	// https://stackoverflow.com/a/3736247
	public static <E> byte[] serialize(E e) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);
		out.writeObject(e);
		return bos.toByteArray();
	}
	
	// Convert a byte[] to a Block object
	public static <E> E deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInputStream in = new ObjectInputStream(bis);
		return (E) in.readObject();
	}	
	
	// Given a socket, return the name of the client connected to that socket
	public static String socketClientName(Socket s) throws UnknownHostException {
		// IP addresses of the nodes in the network
		ArrayList<String> nodeIPs = new ArrayList<String>();
		nodeIPs.add(InetAddress.getByName("node1").getHostAddress());
		nodeIPs.add(InetAddress.getByName("node2").getHostAddress());
		String miner1IP = InetAddress.getByName("miner1").getHostAddress();
		String searchAgent1IP = InetAddress.getByName("search_agent1").getHostAddress();
		
		// Get the IP address of the client
		String clientIP = s.getRemoteSocketAddress().toString();
		clientIP = clientIP.substring(1); // Strip the leading "/"
		clientIP = clientIP.split(":")[0]; // Ignore the port. We only want the IP address.
		
		if (nodeIPs.contains(clientIP)) {
			return "node";
		} else if (clientIP.equals(miner1IP)) {
			return "miner";
		} else if (clientIP.equals(searchAgent1IP)){
			return "search agent";
		} else {
			return "";
		}
	}

}
