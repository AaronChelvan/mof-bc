import static org.fusesource.leveldbjni.JniDBFactory.bytes;
import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.DatatypeConverter;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class Node {
	private static DB db;
	private static Socket clientSocket;
	private static ObjectOutputStream output;
	private static ArrayList<TransactionLocation> myTransactions;
	private static byte[] prevTid;
	
	private static PrivateKey privateKey;
	private static PublicKey publicKey;
	private static String gvs;
	
	private static int originalNumTransactions; // The number of transactions created during the transaction creation phase
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, ClassNotFoundException, InvalidKeySpecException, InvalidKeyException, SignatureException {
		System.out.println("Node is running");
		
		// LevelDB setup
		Options options = new Options();
		options.createIfMissing(true);
		db = factory.open(new File("blockchain"), options);
		
		// myTransactions contains the locations of all transactions created by this node.
		// After a transaction is removed or summarized, it is removed from this node.
		myTransactions = new ArrayList<TransactionLocation>();
		
		// If we are removing or summarizing transactions, populate myTransactions with a list of all transactions created by this node
		if (Config.mode == 1 || Config.mode == 2) {
			// Add all existing transactions to the myTransactions list
			DBIterator iterator = db.iterator();
			for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
				byte[] blockId = iterator.peekNext().getKey();
				Block b;
				if (Config.jsonSerialization == true) {
					b = jsonToBlock(iterator.peekNext().getValue());
				} else {
					b = Util.deserialize(iterator.peekNext().getValue());
				}

				for (Transaction t: b.getTransactions()) {
					myTransactions.add(new TransactionLocation(blockId, t.getTid(), t.getPrevTid()));
				}
			}
			iterator.close();
		}
		db.close();
		originalNumTransactions = myTransactions.size();

		// Load the RSA key pair from the environment variables
		publicKey = Util.stringToPublicKey(System.getenv("PUB_KEY"));
		privateKey = Util.stringToPrivateKey(System.getenv("PRIV_KEY"));

		// Load the Generator Verifier Secret (GVS)
		gvs = System.getenv("GVS");
		
		// Initialize the previous transaction ID to an empty string
		prevTid = new byte[0];
		
		// Thread for sending transactions
		Runnable sendTransactionRunnable = new Runnable() {
			public void run() {
				try {
					sendTransaction();
				} catch (IOException | ClassNotFoundException | InvalidKeyException | NoSuchAlgorithmException | SignatureException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(sendTransactionRunnable, 0, 1, TimeUnit.MILLISECONDS); // How often the node should create a transaction		
		
		// Socket setup
		ServerSocket nodeSocket = new ServerSocket(8000);
		
		// Listen for blocks from the miner
		while (true) {
			Socket connectionSocket = nodeSocket.accept();
			ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream());
			Block b = (Block) in.readObject();
			
			db = factory.open(new File("blockchain"), options);
			if (db.get(b.getBlockId()) == null) {
				System.out.println("Received new block from the miner");
			} else {
				System.out.println("Received updated block from the miner");
			}
			
			// Add the block to this node's blockchain database
			if (Config.jsonSerialization == true) {
				// Convert the block to JSON 
				byte[] jsonStr = blockToJson(b);
				System.out.println(new String(jsonStr)); 
				db.put(b.getBlockId(), jsonStr);
			} else {
				db.put(b.getBlockId(), Util.serialize(b));
			}
			
			db.close();
		}
	}
	
	private static void sendTransaction() throws IOException, ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, InterruptedException {
		// Create a standard transaction, or a remove transaction
		Transaction toSend = null;
		TransactionType nextTransactionType = getNextTransactionType();
		byte[] gv = computeGv(prevTid, true);
		
		if (nextTransactionType == TransactionType.Standard) { // Create a standard transaction
			byte[] randomMessage = new byte[Config.dataSize]; // Generate a random string
			new Random().nextBytes(randomMessage);
			
			HashMap<String, byte[]> transactionData = new HashMap<String, byte[]>();
			transactionData.put("data", randomMessage);
			
			toSend = new Transaction(transactionData, gv, prevTid, TransactionType.Standard);
		
		} else if (nextTransactionType == TransactionType.Remove) { // Create a remove transaction
			System.out.println("sending a remove transaction");
			
			// Once we are done removing transactions, wait indefinitely
			if (myTransactions.size() <= originalNumTransactions * (1-Config.removalPercentage)) {
				System.out.println("Done sending transactions");
				while (true) {
					TimeUnit.MINUTES.sleep(1);
				}
			}
			// Pick a transaction at random
			TransactionLocation tl = myTransactions.get(new Random().nextInt(myTransactions.size()));
			
			// Remove that transaction location from the list
			myTransactions.remove(tl);
			
			// Create a remove transaction
			HashMap<String, byte[]> transactionData = new HashMap<String, byte[]>();
			transactionData.put("location", Util.serialize(tl));
			transactionData.put("pubKey", Util.serialize(publicKey));
			transactionData.put("unsignedGv", computeGv(tl.getPrevTransactionID(), false));
			byte[] sigMessage = new byte[20]; // Generate a signature message
			new Random().nextBytes(sigMessage);
			transactionData.put("sigMessage", sigMessage);
			transactionData.put("sig", Util.sign(privateKey, sigMessage));
			
			toSend = new Transaction(transactionData, gv, prevTid, TransactionType.Remove);
			
		} else if (nextTransactionType == TransactionType.Summary) {
			System.out.println("Sending a summary transaction");
			
			// If there are no more transactions to summarize, wait indefinitely
			if (myTransactions.size() == 0) {
				System.out.println("Done sending transactions");
				while (true) {
					TimeUnit.MINUTES.sleep(1);
				}
			}
			
			// Contains the list of transactions to summarize
			ArrayList<TransactionLocation> transactionsToSummarize = new ArrayList<TransactionLocation>();
			ArrayList<byte[]> prevTids = new ArrayList<byte[]>();
			// Pick some random transactions to summarize
			for (int i = 0; i < Config.numTransactionsInSummary; i++) {
				// Pick a transaction at random
				TransactionLocation tl = myTransactions.get(new Random().nextInt(myTransactions.size()));
				
				// Remove that transaction location from the list
				myTransactions.remove(tl);
				transactionsToSummarize.add(tl);
				
				prevTids.add(computeHash(tl.getPrevTransactionID()));
				
				if (myTransactions.size() == 0) {
					break;
				}
			}
			
			// Create a summary transaction
			HashMap<String, byte[]> transactionData = new HashMap<String, byte[]>();
			transactionData.put("locations", Util.serialize(transactionsToSummarize));
			transactionData.put("pubKey", Util.serialize(publicKey));
			transactionData.put("gvsHash", computeHash(gvs.getBytes()));
			transactionData.put("prevTids", Util.serialize(prevTids));
			byte[] sigMessage = new byte[20]; // Generate a signature message
			new Random().nextBytes(sigMessage);
			transactionData.put("sigMessage", sigMessage);
			transactionData.put("sig", Util.sign(privateKey, sigMessage));
			// TODO - Add summary time, transorder, hash(GVS), and list of hash(P.T.ID)
			
			toSend = new Transaction(transactionData, gv, prevTid, TransactionType.Summary);

		} else { // Invalid transaction type
			System.out.println("Invalid transaction type");
			System.exit(0);
		}
		
		// Send the transaction to the miner
		if (toSend != null) {
			if (toSend.getType() == TransactionType.Standard) {
				System.out.println("Sent std tx = " + DatatypeConverter.printHexBinary(toSend.getTid()));
			} else if (toSend.getType() == TransactionType.Remove) {
				System.out.println("Sent remove tx = " + DatatypeConverter.printHexBinary(toSend.getTid()));
			} else if (toSend.getType() == TransactionType.Summary) {
				System.out.println("Sent summary tx = " + DatatypeConverter.printHexBinary(toSend.getTid()));
			} else {
				System.out.println("Invalid transaction type");
				System.exit(0);
			}
			
			clientSocket = Util.connectToServerSocket("miner", 8000);
			output = new ObjectOutputStream(clientSocket.getOutputStream());
			output.writeObject(toSend);
			clientSocket.close();
			
			prevTid = toSend.getTid();
		}
		
	}
	
	// Compute a GV
	private static byte[] computeGv(byte[] prevTid, boolean signed) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		MessageDigest md = null;
		md = MessageDigest.getInstance("SHA-256");
		md.update(computeHash(gvs.getBytes()));
		md.update(computeHash(prevTid));
		
		if (signed == true) {
			return Util.sign(privateKey, md.digest());
		} else { // unsigned
			return md.digest();
		}
	}
	
	// Given an input, return the SHA-256 hash of it
	private static byte[] computeHash(byte[] input) throws NoSuchAlgorithmException {
		MessageDigest md = null;
		md = MessageDigest.getInstance("SHA-256");
		md.update(input);
		return md.digest();
	}

	// Call this function to determine what type of transaction the node should create next.
	// Can be modified to increase how often a remove transaction is created, etc.
	private static TransactionType getNextTransactionType() {
		if (Config.mode == 0) {
			return TransactionType.Standard;
		} else if (Config.mode == 1) {
			return TransactionType.Remove;
		} else if (Config.mode == 2){
			return TransactionType.Summary;
		} else {
			System.out.println("Mode is invalid");
			System.exit(0);
			return TransactionType.Standard;
		}
	}

	private static byte[] blockToJson(Block b) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule("CustomBlockSerializer", new Version(1, 0, 0, null, null, null));
		module.addSerializer(Block.class, new CustomBlockSerializer());
		mapper.registerModule(module);
		return mapper.writeValueAsBytes(b);
	}
	
	private static Block jsonToBlock(byte[] b) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule("CustomBlockDeserializer", new Version(1, 0, 0, null, null, null));
		module.addDeserializer(Block.class, new CustomBlockDeserializer());
		mapper.registerModule(module);
		return mapper.readValue(b, Block.class);
	}

}


