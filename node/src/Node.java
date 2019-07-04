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
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

public class Node {
	private static DB db;
	private static Socket clientSocket;
	private static ObjectOutputStream output;
	private static int transactionTypeCounter;
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, ClassNotFoundException {
		System.out.println("Node is running");
		
		// LevelDB setup
		Options options = new Options();
		options.createIfMissing(true);
		db = factory.open(new File("blockchain"), options);

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
				} catch (IOException | ClassNotFoundException e) {
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
	
	private static void sendTransaction(String publicKeyStr) throws IOException, ClassNotFoundException {
		
		// Create a standard transaction, or a remove transaction
		Transaction toSend = null;
		TransactionType nextTransactionType = getNextTransactionType();
		
		if (nextTransactionType == TransactionType.Standard) { // Create a standard transaction
			String transactionData = UUID.randomUUID().toString(); // Generate a random string
			toSend = new Transaction(transactionData, publicKeyStr, TransactionType.Standard);
		
		} else if (nextTransactionType == TransactionType.Remove) { // Create a remove transaction
			// Search the blockchain and pick a transaction to remove
			DBIterator iterator = db.iterator();
			boolean found = false; // Have we found a transaction we want to remove?
			TransactionLocation tl = null; // Should contain the transactionLocation of the transaction to be removed
			
			for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
				String blockID = new String(iterator.peekNext().getKey());
				Block block = Util.deserialize(iterator.peekNext().getValue());
				
				for (Transaction t: block.getTransactions()) {
					if (t.getPubKey().equals(publicKeyStr)) {
						tl = new TransactionLocation(blockID, t.getTid());
						found = true;
						break;
					}
				}
				if (found == true) break;
			}
			
			if (found == true) {
				toSend = new Transaction("", publicKeyStr, TransactionType.Remove);
				toSend.addLocation(tl);
			}
			iterator.close();
			
		} else { // Invalid transaction type
			System.out.println("Invalid transaction type");
			System.exit(0);
		}
		
		// Send the transaction to the miner
		if (toSend != null) {
			clientSocket = Util.connectToServerSocket("miner", 8000);
			output = new ObjectOutputStream(clientSocket.getOutputStream());
			output.writeObject(toSend);
			clientSocket.close();
		}
		
	}
	
	// Call this function to determine what type of transaction the node should create next.
	// Can be modified to increase how often a remove transaction is created, etc.
	private static TransactionType getNextTransactionType() {
		if (transactionTypeCounter == 2) {
			transactionTypeCounter = 0;
			return TransactionType.Remove;
		} else {
			transactionTypeCounter++;
			return TransactionType.Standard;
		}
	}

}