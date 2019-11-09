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
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	private static Lock dbLock;
	private static Options options;
	private static Socket clientSocket;
	private static ObjectOutputStream output;
	private static ArrayList<TransactionLocation> myTransactions;
	private static byte[] prevTid;
	
	private static PrivateKey privateKey;
	private static PublicKey publicKey;
	private static String gvs;
	
	private static int originalNumTransactions; // The number of transactions created during the transaction creation phase
	private static long startTime; // used for recording timings
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, ClassNotFoundException, InvalidKeySpecException, InvalidKeyException, SignatureException {
		System.out.println("Node is running");
		
		// LevelDB setup
		options = new Options();
		options.createIfMissing(true);
		db = factory.open(new File("blockchain"), options);
		dbLock = new ReentrantLock();
		
		// myTransactions contains the locations of all transactions created by this node.
		// After a transaction is removed or summarized, it is removed from this node.
		myTransactions = new ArrayList<TransactionLocation>();
		
		// Convert the blockchain from Java serialization to JSON and exit
		if (Config.mode == 3) {
			DBIterator iterator = db.iterator();
			for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
				Block b = Util.deserialize(iterator.peekNext().getValue());
				startTime = System.nanoTime();
				byte[] jsonStr = blockToJson(b);
				System.out.println("Time to convert block to JSON: " + (System.nanoTime() - startTime) + "ns");
				//System.out.println(new String(jsonStr)); 
				db.put(b.getBlockId(), jsonStr);
			}
			iterator.close();
			db.close();
			System.exit(0);
		}
		
		// Convert the blockchain from JSON to Java serialization and exit
		if (Config.mode == 4) { 
			DBIterator iterator = db.iterator();
			for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
				startTime = System.nanoTime();
				Block b = jsonToBlock(iterator.peekNext().getValue());
				System.out.println("Time to convert JSON to block: " + (System.nanoTime() - startTime) + "ns");
				db.put(b.getBlockId(), Util.serialize(b));
			}
			iterator.close();
			db.close();
			System.exit(0);
		}
		
		// Convert the blockchain from Java serialization to CSV and exit
		if (Config.mode == 5) {
			DBIterator iterator = db.iterator();
			for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
				Block b = Util.deserialize(iterator.peekNext().getValue());
				startTime = System.nanoTime();
				byte[] csvStr = blockToCsv(b);
				System.out.println("Time to convert block to CSV: " + (System.nanoTime() - startTime) + "ns"); 
				db.put(b.getBlockId(), csvStr);
			}
			iterator.close();
			db.close();
			System.exit(0);
		}
		
		// Convert the blockchain from CSV to Java serialization and exit
		if (Config.mode == 6) {
			DBIterator iterator = db.iterator();
			for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
				startTime = System.nanoTime();
				Block b = csvToBlock(iterator.peekNext().getValue());
				System.out.println("Time to convert CSV to block: " + (System.nanoTime() - startTime) + "ns");
				db.put(b.getBlockId(), Util.serialize(b));
			}
			iterator.close();
			db.close();
			System.exit(0);
		}
		
		// If we are removing or summarizing transactions, populate myTransactions with a list of all transactions created by this node
		if (Config.mode == 1 || Config.mode == 2) {
			// Add all existing transactions to the myTransactions list
			DBIterator iterator = db.iterator();
			for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
				byte[] blockId = iterator.peekNext().getKey();
				Block b = Util.deserialize(iterator.peekNext().getValue());

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
		prevTid = ".".getBytes();
		
		// Thread for sending transactions
		startTime = System.nanoTime();
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
			
			dbLock.lock();
			db = factory.open(new File("blockchain"), options);
			/*
			if (db.get(b.getBlockId()) == null) {
				System.out.println("Received new block from the miner");
			} else {
				System.out.println("Received updated block from the miner");
			}
			*/
			
			// Add the block to this node's blockchain database
			db.put(b.getBlockId(), Util.serialize(b));
			
			db.close();
			dbLock.unlock();
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
			//System.out.println("sending a remove transaction");
			
			// Once we are done removing transactions, wait indefinitely
			if (myTransactions.size() <= originalNumTransactions * (1-Config.removalPercentage)) {
				System.out.println("Done sending remove transactions");
				System.out.println("Time taken: " + (System.nanoTime() - startTime) + "ns");
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
			//System.out.println("Sending a summary transaction");
			
			// If there are no more transactions to summarize, wait indefinitely
			if (myTransactions.size() == 0) {
				System.out.println("Done sending summary transactions");
				System.out.println("Time taken: " + (System.nanoTime() - startTime) + "ns");
				while (true) {
					TimeUnit.MINUTES.sleep(1);
				}
			}
			
			// Contains the list of transactions to summarize
			ArrayList<TransactionLocation> transactionsToSummarize = new ArrayList<TransactionLocation>();
			ArrayList<byte[]> prevTids = new ArrayList<byte[]>();
			ArrayList<byte[]> tids = new ArrayList<byte[]>();
			ArrayList<String> timestamps = new ArrayList<String>();
			// Pick some random transactions to summarize
			for (int i = 0; i < Config.numTransactionsInSummary; i++) {
				// Pick a transaction at random
				TransactionLocation tl = myTransactions.get(new Random().nextInt(myTransactions.size()));
				
				// Remove that transaction location from the list
				myTransactions.remove(tl);
				transactionsToSummarize.add(tl);
				
				// Get the previous transaction id
				prevTids.add(computeHash(tl.getPrevTransactionID()));
				
				// Get the transaction id
				tids.add(tl.getTransactionID());
				
				// Get the timestamp
				dbLock.lock();
				db = factory.open(new File("blockchain"), options);
				DBIterator iterator = db.iterator();
				boolean found = false;
				for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
					byte[] blockId = iterator.peekNext().getKey();
					if (Arrays.equals(tl.getBlockID(), blockId)) {
						Block b = Util.deserialize(iterator.peekNext().getValue());
						for (Transaction t: b.getTransactions()) {
							if (Arrays.equals(t.getTid(), tl.getTransactionID())) {
								timestamps.add(t.getTimestamp());
								found = true;
								break;
							}
						}
						if (found == true) break;
					}
				}
				if (found == false) {
					System.out.println("Could not find timestamp");
					System.exit(0);
				}
				iterator.close();
				db.close();
				dbLock.unlock();
				
				if (myTransactions.size() == 0) {
					break;
				}
			}
			
			// Find the minimum number of unique bytes needed in the list of transaction IDs
			int trimmedLength = 0;
			for (int length = 1; length < tids.get(0).length; length++) {
				Set<String> set = new HashSet<String>();
				boolean failed = false;
				for (byte[] t: tids) {
					byte[] slice = Arrays.copyOfRange(t, 0, length+1);
					if (set.contains(new String(slice))) {
						failed = true; // failed to find a valid length
						break;
					} else {
						set.add(new String(slice));
					}
				}
				
				// If we have found a valid length
				if (failed == false) {
					trimmedLength = length;
					break;
				}
			}
			
			if (trimmedLength != 0) {
				// Trim the length of the transaction IDs
				for (int i = 0; i < tids.size(); i++) {
					byte[] newTid = Arrays.copyOfRange(tids.get(i), 0, trimmedLength+1);
					tids.set(i, newTid);
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
			transactionData.put("summaryTime", Util.serialize(timestamps));
			transactionData.put("transorder", Util.serialize(tids));
			
			toSend = new Transaction(transactionData, gv, prevTid, TransactionType.Summary);

		} else { // Invalid transaction type
			System.out.println("Invalid transaction type");
			System.exit(0);
		}
		
		// Send the transaction to the miner
		if (toSend != null) {
			if (toSend.getType() == TransactionType.Standard) {
				//System.out.println("Sent std tx = " + DatatypeConverter.printHexBinary(toSend.getTid()));
			} else if (toSend.getType() == TransactionType.Remove) {
				//System.out.println("Sent remove tx = " + DatatypeConverter.printHexBinary(toSend.getTid()));
			} else if (toSend.getType() == TransactionType.Summary) {
				//System.out.println("Sent summary tx = " + DatatypeConverter.printHexBinary(toSend.getTid()));
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
	
	private static byte[] blockToCsv(Block b) {
		StringBuilder csv = new StringBuilder();
		// The first line contains the block ID and the previous block ID
		csv.append(encodeBase64(b.getBlockId()));
		csv.append(",");
		csv.append(encodeBase64(b.getPrevBlockId()));
		csv.append("\n");
		
		// Iterate through the transactions
		for (Transaction t: b.getTransactions()) {
			csv.append(encodeBase64(t.getTid()));
			csv.append(",");
			csv.append(encodeBase64(t.getPrevTid()));
			if (t.getType() == TransactionType.Standard) {
				HashMap<String, byte[]> data = t.getData();
				csv.append(",");
				csv.append("standard");
				csv.append(",");
				csv.append(encodeBase64(t.getGv()));
				csv.append(",");
				csv.append(t.getTimestamp());
				csv.append(",");
				csv.append(encodeBase64(data.get("data")));				
			} else if (t.getType() == TransactionType.Remove) {
				HashMap<String, byte[]> data = t.getData();
				csv.append(",");
				csv.append("remove");
				csv.append(",");
				csv.append(encodeBase64(t.getGv()));
				csv.append(",");
				csv.append(t.getTimestamp());
				csv.append(",");
				csv.append(encodeBase64(data.get("location")));
				csv.append(",");
				csv.append(encodeBase64(data.get("pubKey")));
				csv.append(",");
				csv.append(encodeBase64(data.get("unsignedGv")));
				csv.append(",");
				csv.append(encodeBase64(data.get("sigMessage")));
				csv.append(",");
				csv.append(encodeBase64(data.get("sig")));				
			} else if (t.getType() == TransactionType.Summary) {
				HashMap<String, byte[]> data = t.getData();
				csv.append(",");
				csv.append("summary");
				csv.append(",");
				csv.append(encodeBase64(t.getGv()));
				csv.append(",");
				csv.append(t.getTimestamp());
				csv.append(",");
				csv.append(encodeBase64(data.get("locations")));
				csv.append(",");
				csv.append(encodeBase64(data.get("pubKey")));
				csv.append(",");
				csv.append(encodeBase64(data.get("gvsHash")));
				csv.append(",");
				csv.append(encodeBase64(data.get("prevTids")));
				csv.append(",");
				csv.append(encodeBase64(data.get("sig")));
				csv.append(",");
				csv.append(encodeBase64(data.get("sigMessage")));
				csv.append(",");
				csv.append(encodeBase64(data.get("summaryTime")));
				csv.append(",");
				csv.append(encodeBase64(data.get("transorder")));				
			}
			csv.append("\n");
		}
		return csv.toString().getBytes();
	}
	
	private static Block csvToBlock(byte[] csv) {
		String csvString = new String(csv);
		String[] lines = csvString.split("\n");
		
		Block b = new Block();
		// The first line contains the block ID and the previous block ID
		String[] parts = lines[0].split(",");
		b.setBlockId(decodeBase64(parts[0]));
		b.setPrevBlockId(decodeBase64(parts[1]));
		
		// The remaining lines are transactions
		for (int i = 1; i < lines.length; i++) {
			Transaction t = new Transaction();
			parts = lines[i].split(",");
			t.setTid(decodeBase64(parts[0]));
			t.setPrevTid(decodeBase64(parts[1]));
			
			// If this is a transaction doesn't have any more fields
			// because they have been removed, go to the next line
			if (parts.length == 2) {
				continue;
			}
			
			if (parts[2].equals("standard")) {
				t.setType(TransactionType.Standard);
				t.setGv(decodeBase64(parts[3]));
				t.setTimestamp(parts[4]);
				HashMap<String, byte[]> data = new HashMap<String, byte[]>();
				data.put("data", decodeBase64(parts[5]));
				t.setData(data);
			} else if (parts[2].equals("remove")) {
				t.setType(TransactionType.Remove);
				t.setGv(decodeBase64(parts[3]));
				t.setTimestamp(parts[4]);
				HashMap<String, byte[]> data = new HashMap<String, byte[]>();
				data.put("location", decodeBase64(parts[5]));
				data.put("pubKey", decodeBase64(parts[6]));
				data.put("unsignedGv", decodeBase64(parts[7]));
				data.put("sigMessage", decodeBase64(parts[8]));
				data.put("sig", decodeBase64(parts[9]));	
				t.setData(data);
			} else if (parts[2].equals("summary")) {
				t.setType(TransactionType.Summary);
				t.setGv(decodeBase64(parts[3]));
				t.setTimestamp(parts[4]);
				HashMap<String, byte[]> data = new HashMap<String, byte[]>();
				data.put("locations", decodeBase64(parts[5]));
				data.put("pubKey", decodeBase64(parts[6]));
				data.put("gvsHash", decodeBase64(parts[7]));
				data.put("prevTids", decodeBase64(parts[8]));
				data.put("sig", decodeBase64(parts[9]));
				data.put("sigMessage", decodeBase64(parts[10]));
				data.put("summaryTime", decodeBase64(parts[11]));
				data.put("transorder", decodeBase64(parts[12]));
				t.setData(data);
			}
			
			b.addTransaction(t);
		}
		
		return b;
	}
	
	private static String encodeBase64(byte[] input) {
		return Base64.getEncoder().encodeToString(input);
	}

	private static byte[] decodeBase64(String input) {
		return Base64.getDecoder().decode(input.getBytes());
	}

}


