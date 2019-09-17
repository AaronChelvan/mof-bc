import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
		String summaryManagerAgentIP = InetAddress.getByName("summary_manager_agent").getHostAddress();
		
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
		} else if (clientIP.equals(summaryManagerAgentIP)){
			return "summary_manager_agent";
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
	public static PublicKey stringToPublicKey(String keyStr) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] keyBytes = Base64.getDecoder().decode(keyStr);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(keySpec);
	}
	
	// Accepts a key that has been encoded as a string and returns a public key
	public static PrivateKey stringToPrivateKey(String keyStr) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] keyBytes = Base64.getDecoder().decode(keyStr);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
	}
	
	// Converts a key to a string
	public static String keyToString(Key k) {
		return Base64.getEncoder().encodeToString(k.getEncoded());
	}
	
	// Given a public key and a message, encrypt the message
	public static byte[] encrypt(PublicKey k, byte[] message) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, k);
		return cipher.doFinal(message);
	}
	
	// Given a private key and an encrypted message, decrypt it
	public static byte[] decrypt(PrivateKey k, byte[] encryptedMessage) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, k);
		return cipher.doFinal(encryptedMessage);
	}
	
	// Given a private key and a message, create a signature
	public static byte[] sign(PrivateKey k, byte[] message) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
		Signature rsa = Signature.getInstance("SHA1withRSA"); 
		rsa.initSign(k);
		rsa.update(message);
		return rsa.sign();
	}
	
	// Given a public key, a message, and a signature, verify if the signature is valid
	public static boolean verify(PublicKey k, byte[] message, byte[] signature) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature sig = Signature.getInstance("SHA1withRSA");
		sig.initVerify(k);
		sig.update(message);
		return sig.verify(signature);
	}

}
