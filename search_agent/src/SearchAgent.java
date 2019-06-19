import static org.fusesource.leveldbjni.JniDBFactory.bytes;
import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

public class SearchAgent {
	private static DB db; // LevelDB database used for storing the blockchain

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// LevelDB setup
		Options options = new Options();
		options.createIfMissing(true);
		db = factory.open(new File("blockchain"), options);
		
		// Socket setup
		ServerSocket agentSocket = new ServerSocket(8000);
		
		// Cleaning period setup
		int cleaningPeriod = 30; // (Seconds)
		Runnable blockchainScan = new Runnable() {
			public void run() {
				try {
					searchBlockchain();
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			}
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(blockchainScan, 0, cleaningPeriod, TimeUnit.SECONDS);
		
		System.out.println("Setup complete");
		
		// Listen for incoming connections
		while (true) {
			// Receive Blocks from the miner or one of the agents
			Socket connectionSocket = agentSocket.accept();
			ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream());
			Block currentBlock = (Block) in.readObject();
			
			if (Util.socketClientName(connectionSocket).equals("miner1")) {
				// Scan the block for remove transactions & summary transactions
				for (Transaction t: currentBlock.getTransactions()) {
					if (t.getType() == TransactionType.Remove) {
						// Extract the location of the transaction to be removed
						TransactionLocation tl = Util.deserialize(bytes(t.getData()));
						
						// Get the block containing the transaction
						Block b = Util.deserialize(db.get(bytes(tl.getBlockID())));
						
						// Find the transaction to be removed
						for (Transaction t2: b.getTransactions()) {
							if (t2.getTid().equals(tl.getTransactionID())) {
								// Delete everything in this transaction except for the transaction ID and previous transaction ID
								t2.clearTransaction();
								break;
							}
						} 
						
						// Put the updated block back in the database
						db.put(bytes(b.getBlockId()), Util.serialize(b));
						
					} else if (t.getType() == TransactionType.Summary) {
						// TODO
						
					}
				}
			} else {
				//TODO - if the client is an agent, then this is an updated block
				//Check that this block already exists in the blockchain
				System.out.println("Something went wrong");
			}
			
			// Add the block to the blockchain
			db.put(bytes(currentBlock.getBlockId()), Util.serialize(currentBlock));
		}
	}
	
	private void transmitUpdatedBlock(Block b) {
		
	}

}
