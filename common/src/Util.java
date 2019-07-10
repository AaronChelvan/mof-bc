import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

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
		//nodeIPs.add(InetAddress.getByName("node2").getHostAddress());
		String minerIP = InetAddress.getByName("miner").getHostAddress();
		String searchAgentIP = InetAddress.getByName("search_agent").getHostAddress();
		String serviceAgentIP = InetAddress.getByName("service_agent").getHostAddress();
		
		// Get the IP address of the client
		String clientIP = s.getRemoteSocketAddress().toString();
		clientIP = clientIP.substring(1); // Strip the leading "/"
		clientIP = clientIP.split(":")[0]; // Ignore the port. We only want the IP address.
		
		if (nodeIPs.contains(clientIP)) {
			return "node";
		} else if (clientIP.equals(minerIP)) {
			return "miner";
		} else if (clientIP.equals(searchAgentIP)){
			return "search_agent";
		} else if (clientIP.equals(serviceAgentIP)){
			return "service_agent";
		} else {
			return "";
		}
	}
	
	// Given the IP address and port number of a server,
	// return a client socket that is connected to the server
	public static Socket connectToServerSocket(String ip, int port) {
		Socket clientSocket = null;
		boolean connected = false;
		while (!connected) {
			try {
				clientSocket = new Socket(ip, port);
				connected = true;
			} catch (Exception e) {
				// If connection was unsuccessful, wait 1 second and try again
				System.out.println(e);
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e1) {
					System.out.println(e1);
				}
			}
		}
		
		return clientSocket;
	}
	
	// Accepts a key that has been encoded as a string and returns a public key
	public static Key stringToPublicKey(String keyStr) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] keyBytes = Base64.getDecoder().decode(keyStr);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(keySpec);
	}
	
	// Accepts a key that has been encoded as a string and returns a public key
	public static Key stringToPrivateKey(String keyStr) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] keyBytes = Base64.getDecoder().decode(keyStr);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
	}
	
	// Converts a key to a string
	public static String keyToString(Key k) {
		return Base64.getEncoder().encodeToString(k.getEncoded());
	}
	

}
