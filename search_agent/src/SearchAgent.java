import static org.fusesource.leveldbjni.JniDBFactory.bytes;
import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

public class SearchAgent {
	private static DB db; // LevelDB database used for storing the blockchain
	private static HashMap<String, Block> updatedBlocks; // Key = block ID. Val = block.
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// LevelDB setup
		Options options = new Options();
		options.createIfMissing(true);
		db = factory.open(new File("blockchain"), options);
		
		// Socket setup
		ServerSocket agentSocket = new ServerSocket(8000);
		
		// Cleaning period setup
		int cleaningPeriod = 10; // (Seconds)
		Runnable blockchainScan = new Runnable() {
			public void run() {
				try {
					transmitUpdatedBlocks();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(blockchainScan, cleaningPeriod, cleaningPeriod, TimeUnit.SECONDS);
		
		// A list of blocks that have been updated over the last cleaning period
		updatedBlocks = new HashMap<String, Block>(); // Key = block ID. Val = block.
		
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
			
			// Scan the block for remove transactions & summary transactions
			for (Transaction t: currentBlock.getTransactions()) {
				if (t.getType() == TransactionType.Remove) {
					// TODO - Verify if the creator of this transaction is the same node
					// that created the transaction that is to be removed
					
					// Extract the location of the transaction to be removed
					TransactionLocation tl = Util.deserialize(bytes(t.getData()));
					
					// If the block to be updated is not already in updatedBlocks list,
					// get it from the database
					Block b;
					if (!updatedBlocks.containsKey(tl.getBlockID())) {
						b = Util.deserialize(db.get(bytes(tl.getBlockID())));
					} else {
						b = updatedBlocks.get(tl.getBlockID());
					}
					
					// Find the transaction to be removed
					for (Transaction t2: b.getTransactions()) {
						if (t2.getTid().equals(tl.getTransactionID())) {
							// Delete everything in this transaction except for the transaction ID and previous transaction ID
							t2.clearTransaction();
							break;
						}
					}
					
				} else if (t.getType() == TransactionType.Summary) {
					// TODO
					
				}
			}
			
			// Add the block to the blockchain
			db.put(bytes(currentBlock.getBlockId()), Util.serialize(currentBlock));
			
		}
	}
	
	private static void transmitUpdatedBlocks() throws UnknownHostException, IOException {
		// Establish a connection to the miner
		Socket clientSocket = Util.connectToServerSocket("miner1", 8000);
		ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
		
		// Transmit the updated blocks to the miner
		output.writeObject(updatedBlocks);
		
		// Close the connection
		clientSocket.close();
		System.out.println("Transmitted updated blocks");
		
		// Write the updated blocks to the database
		for (Block b : updatedBlocks.values()) {
			db.put(bytes(b.getBlockId()), Util.serialize(b));
		}
		
		// Clear the list of updated blocks
		updatedBlocks.clear();
	}

}
