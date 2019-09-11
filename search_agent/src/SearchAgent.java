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
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.DatatypeConverter;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
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
				System.out.println("Received new block from miner");
				
				// Add the block to the blockchain
				db.put(currentBlock.getBlockId(), Util.serialize(currentBlock));
				
				// Scan the block for remove transactions & summary transactions
				for (Transaction t: currentBlock.getTransactions()) {
					if (t.getType() == TransactionType.Remove) {
						// Extract the location of the transaction to be removed
						HashMap<String, byte[]> transactionData = t.getData();
						TransactionLocation tl = Util.deserialize(transactionData.get("location"));
						System.out.println("Found transaction to remove = " + DatatypeConverter.printHexBinary(tl.getTransactionID()));
						
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
						
						PublicKey pubKey = Util.deserialize(transactionData.get("pubKey"));
						if (!Util.verify(pubKey, transactionData.get("unsignedGv"), toRemove.getGv())) {
							System.out.println("Failed 1st verification check");
							continue;
						}
						
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
						// TODO
						System.exit(0);
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
