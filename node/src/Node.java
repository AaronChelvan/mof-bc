import static org.fusesource.leveldbjni.JniDBFactory.bytes;
import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

public class Node {
	private static Socket clientSocket;
	private static ObjectOutputStream output;
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, ClassNotFoundException {
		System.out.println("Node is running");
		
		// LevelDB setup
		Options options = new Options();
		options.createIfMissing(true);
		DB db = factory.open(new File("blockchain"), options);

		// Generate an RSA key pair
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.generateKeyPair();
		Key publicKey = kp.getPublic();
		Key privateKey = kp.getPrivate();

		// Convert the keys to strings
		String publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());
		String privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());

		// Thread for sending transactions
		Runnable blockchainScan = new Runnable() {
			public void run() {
				try {
					sendTransaction(publicKeyStr);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(blockchainScan, 0, 1, TimeUnit.MILLISECONDS);
				
		// Socket setup
		ServerSocket nodeSocket = new ServerSocket(8000);
		
		// Listen for blocks from the miner
		while (true) {
			Socket connectionSocket = nodeSocket.accept();
			ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream());
			Block b = (Block) in.readObject();
			db.put(bytes(b.getBlockId()), Util.serialize(b));
			System.out.println("Received block from the miner");
		}
		
	}
	
	private static void sendTransaction(String publicKeyStr) throws IOException {
		// Establish a connection to the miner
		clientSocket = Util.connectToServerSocket("miner", 8000);
		output = new ObjectOutputStream(clientSocket.getOutputStream());
		
		// Create a transaction and send it
		String transactionData = UUID.randomUUID().toString(); // Generate a random string
		Transaction toSend = new Transaction(transactionData, publicKeyStr, TransactionType.Standard);
		output.writeObject(toSend);
		//System.out.println("Sent transaction");
		clientSocket.close();
	}

}