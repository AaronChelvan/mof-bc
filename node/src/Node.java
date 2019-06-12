import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Node {
	
	public static void main(String[] args) throws NoSuchAlgorithmException {
		System.out.println("Node is running");

		// Generate an RSA key pair
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.generateKeyPair();
		Key publicKey = kp.getPublic();
		Key privateKey = kp.getPrivate();

		// Convert the keys to strings
		String publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());
		String privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());

		while (true) {
			try {
				// Establish a connection to the miner
				Socket clientSocket = new Socket("miner1", 8000);
				
				// Create a transaction, serialize it, and send it
				ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
				String transactionData = UUID.randomUUID().toString(); // Generate a random string
				Transaction toSend = new Transaction(transactionData, publicKeyStr, TransactionType.Standard);
				output.writeObject(toSend);
				
				// Close the connection
				clientSocket.close();
			} catch (Exception e){
				System.out.println(e);
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException timeError) {
					System.out.println(timeError);
				}
			}
		}
	}

}