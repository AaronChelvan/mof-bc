import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

public class SearchAgent {
	private static DB db; // LevelDB database used for storing the blockchain
	
	public static void main(String[] args) throws IOException, ClassNotFoundException, InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		// LevelDB setup
		Options options = new Options();
		options.createIfMissing(true);
		
		// Socket setup
		ServerSocket agentSocket = new ServerSocket(8000);
		
		System.out.println("Setup complete");	
		
		// Listen for incoming connections
		while (true) {
			// Receive Blocks from the miner
			Socket connectionSocket = agentSocket.accept();
			ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream());
			Block currentBlock = (Block) in.readObject();
			
			if (!Util.socketClientName(connectionSocket).equals("miner")) {
				System.out.println("Something went wrong!");
				System.exit(0);
			}
			
			db = factory.open(new File("blockchain"), options);
			
			// If this block is already in the database, then it is an updated
			// block which has already had its remove and summary transactions processed
			if (db.get(currentBlock.getBlockId()) == null) {
				// Add the block to the blockchain
				db.put(currentBlock.getBlockId(), Util.serialize(currentBlock));
				
				// Scan the block for remove transactions & summary transactions
				for (Transaction t: currentBlock.getTransactions()) {
					if (t.getType() == TransactionType.Remove) {
						// Extract the location of the transaction to be removed
						HashMap<String, byte[]> transactionData = t.getData();
						TransactionLocation tl = Util.deserialize(transactionData.get("location"));
						
						// Verify if the creator of this transaction is the same node
						// that created the transaction that is to be removed
						Block toRemoveBlock = Util.deserialize(db.get(tl.getBlockID()));
						Transaction toRemove = null;
						for (Transaction t2: toRemoveBlock.getTransactions()) {
							if (Arrays.equals(t2.getTid(), tl.getTransactionID())) {
								toRemove = t2;
								break;
							}
						}
						
						if (toRemove == null) {
							System.out.println("Something went wrong");
							System.exit(0);
						}
						
						// Check that the GV is valid 
						PublicKey pubKey = Util.deserialize(transactionData.get("pubKey"));
						if (!Util.verify(pubKey, transactionData.get("unsignedGv"), toRemove.getGv())) {
							System.out.println("Failed 1st verification check");
							continue;
						}
						
						// Check that the signature is valid
						if (!Util.verify(pubKey, transactionData.get("sigMessage"), transactionData.get("sig"))) {
							System.out.println("Failed 2nd verification check");
							continue;
						}	
						
						// Send the location to the service agent
						// Open a connection
						Socket clientSocket = Util.connectToServerSocket("service_agent", 8000);
						
						// Transmit the block
						ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
						output.writeObject(tl);
						
						// Close the connection
						clientSocket.close();
						
					} else if (t.getType() == TransactionType.Summary) {
						// Extract the locations of the transactions to be summarized
						HashMap<String, byte[]> transactionData = t.getData();
						ArrayList<TransactionLocation> locations = Util.deserialize(transactionData.get("locations"));
						byte[] gvsHash = transactionData.get("gvsHash");
						ArrayList<byte[]> prevTids = Util.deserialize(transactionData.get("prevTids"));
						ArrayList<TransactionLocation> validLocations = new ArrayList<TransactionLocation>();
						
						// Verify that the creator of this summary transaction also created all of the transactions being summarized
						for (int i = 0; i < locations.size(); i++) {
							// Find the block containing this transaction
							TransactionLocation tl = locations.get(i);
							Block toRemoveBlock = Util.deserialize(db.get(tl.getBlockID()));
							Transaction toRemove = null;
							for (Transaction t2: toRemoveBlock.getTransactions()) {
								if (Arrays.equals(t2.getTid(), tl.getTransactionID())) {
									toRemove = t2;
									break;
								}
							}
							
							if (toRemove == null) {
								System.out.println("Something went wrong");
								System.exit(0);
							}
							
							byte[] prevTid = prevTids.get(i);
							
							// Compute the unsigned GV
							MessageDigest md = null;
							md = MessageDigest.getInstance("SHA-256");
							md.update(gvsHash);
							md.update(prevTid);
							byte[] unsignedGV = md.digest();
							
							// Check that the GV is valid
							PublicKey pubKey = Util.deserialize(transactionData.get("pubKey"));
							if (!Util.verify(pubKey, unsignedGV, toRemove.getGv())) {
								System.out.println("Failed 1st verification check");
								continue;
							}
							
							// Check that the signature is valid
							if (!Util.verify(pubKey, transactionData.get("sigMessage"), transactionData.get("sig"))) {
								System.out.println("Failed 2nd verification check");
								continue;
							}
							validLocations.add(tl);
						}
						
						// Send the locations to the summary manager agent
						// Open a connection
						Socket clientSocket = Util.connectToServerSocket("summary_manager_agent", 8000);
						
						// Transmit the block
						ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
						output.writeObject(validLocations);
						
						// Close the connection
						clientSocket.close();
					}
				}
			} else { // This should not happen
				System.out.println("Received updated block");
				System.exit(0);
			}
			
			db.close();
		}
	}
	
	

}
