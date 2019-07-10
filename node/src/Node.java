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
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.DatatypeConverter;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

public class Node {
	private static DB db;
	private static Socket clientSocket;
	private static ObjectOutputStream output;
	private static int transactionTypeCounter;
	private static ArrayList<TransactionLocation> myTransactions;
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, ClassNotFoundException, InvalidKeySpecException {
		System.out.println("Node is running");
		
		// LevelDB setup
		Options options = new Options();
		options.createIfMissing(true);
		db = factory.open(new File("blockchain"), options);

		// Load the RSA key pair from the environment variables
		String publicKeyStr = System.getenv("PUB_KEY");
		String privateKeyStr = System.getenv("PRIV_KEY");
		
		Key publicKey = Util.stringToPublicKey(publicKeyStr);
		Key privateKey = Util.stringToPrivateKey(privateKeyStr);

		// Thread for sending transactions
		transactionTypeCounter = 0;
		Runnable sendTransactionRunnable = new Runnable() {
			public void run() {
				try {
					sendTransaction(publicKeyStr);
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(sendTransactionRunnable, 0, 1, TimeUnit.SECONDS);
		
		// myTransactions contains the locations of all transactions created by this node.
		// After a transaction is removed or summarized, it is removed from this node.
		myTransactions = new ArrayList<TransactionLocation>();
		
		// Socket setup
		ServerSocket nodeSocket = new ServerSocket(8000);
		
		// Listen for blocks from the miner
		while (true) {
			Socket connectionSocket = nodeSocket.accept();
			ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream());
			Block b = (Block) in.readObject();
			
			// Scan the block for transactions created by this node and add their locations to "myTransactions"
			for (Transaction t: b.getTransactions()) {
				if (t.getPubKey().equals(publicKeyStr)) {
					myTransactions.add(new TransactionLocation(b.getBlockId(), t.getTid()));
				}
			}
			
			// Add the block to this node's blockchain database
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
			System.out.println("sending a remove transaction");
			
			// If there are transactions that can be removed
			if (myTransactions.size() > 0) {
				// Pick a transaction at random
				Random rand = new Random();
				TransactionLocation tl = myTransactions.get(rand.nextInt(myTransactions.size()));
				
				// Remove that transaction location from the list
				myTransactions.remove(tl);
				
				// Add the location of that transaction to the remove transaction
				toSend = new Transaction("", publicKeyStr, TransactionType.Remove);
				toSend.addLocation(tl);
			} else {
				System.out.println("Haven't found any transactions to remove");
			}
			
		} else { // Invalid transaction type
			System.out.println("Invalid transaction type");
			System.exit(0);
		}
		
		// Send the transaction to the miner
		if (toSend != null) {
			if (toSend.getType() == TransactionType.Standard) {
				System.out.println("Sent std tx = " +  toSend);
			} else if (toSend.getType() == TransactionType.Remove) {
				System.out.println("Sent remove tx = " + toSend);
			} else {
				System.out.println("Invalid transaction type");
			}
			
			clientSocket = Util.connectToServerSocket("miner", 8000);
			output = new ObjectOutputStream(clientSocket.getOutputStream());
			output.writeObject(toSend);
			clientSocket.close();
		}
		
	}
	
	// Call this function to determine what type of transaction the node should create next.
	// Can be modified to increase how often a remove transaction is created, etc.
	private static TransactionType getNextTransactionType() {
		if (transactionTypeCounter >= 2) {
			transactionTypeCounter = 0;
			return TransactionType.Remove;
		} else {
			transactionTypeCounter++;
			return TransactionType.Standard;
		}
	}

}