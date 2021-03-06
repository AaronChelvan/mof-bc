import static org.fusesource.leveldbjni.JniDBFactory.factory;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

public class ServiceAgent {
	private static DB db;
	private static Options options;
	private static HashMap<String, Block> updatedBlocks; // Key = block ID. Val = block.
	private static Lock updatedBlocksLock;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// LevelDB setup
		options = new Options();
		options.createIfMissing(true);
		db = factory.open(new File("blockchain"), options);
		
		// Socket setup
		ServerSocket agentSocket = new ServerSocket(8000);
		
		// A list of blocks that have been updated over the last cleaning period
		updatedBlocks = new HashMap<String, Block>(); // Key = block ID. Val = block.
		updatedBlocksLock = new ReentrantLock(); // A lock for accessing updatedBlocks
		
		// Cleaning period setup
		int cleaningPeriod = Config.cleaningPeriod;
		Runnable transmit = new Runnable() {
			public void run() {
				try {
					transmitUpdatedBlocks();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(transmit, cleaningPeriod, cleaningPeriod, TimeUnit.SECONDS);
		System.out.println("Setup complete");
		
		// Listen for incoming connections
		while (true) {
			Socket connectionSocket = agentSocket.accept();
			ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream());			
			
			// Received a block from the miner
			if (Util.socketClientName(connectionSocket).equals("miner")) {
				// Add it to the database
				Block b = (Block) in.readObject();
				db.put(b.getBlockId(), Util.serialize(b));
			
			// Received a remove transaction location from the search agent
			} else if (Util.socketClientName(connectionSocket).equals("search_agent")) {
				TransactionLocation tl = (TransactionLocation) in.readObject();
				// If the block to be updated is not already in updatedBlocks list,
				// get it from the database
				Block b;
				updatedBlocksLock.lock();
				if (!updatedBlocks.containsKey(new String(tl.getBlockID()))) {
					b = Util.deserialize(db.get(tl.getBlockID()));
				} else {
					b = updatedBlocks.get(new String(tl.getBlockID()));
				}
				
				// Find the transaction to be removed
				for (Transaction t: b.getTransactions()) {
					if (Arrays.equals(t.getTid(), tl.getTransactionID())) {
						// Delete everything in this transaction except for the transaction ID and previous transaction ID
						t.clearTransaction();
						break;
					}
				}
				updatedBlocks.put(new String(b.getBlockId()), b);
				updatedBlocksLock.unlock();
			} else {
				// Should not get here
				System.out.println("Received connection from a node that is not a miner or search agent");
				System.exit(0);
			}
		}
	}
	
	private static void transmitUpdatedBlocks() throws UnknownHostException, IOException {
		updatedBlocksLock.lock();
		if (updatedBlocks.size() == 0) {
			System.out.println("No blocks have been updated");
			updatedBlocksLock.unlock();
			return;
		}
		
		// Establish a connection to the miner
		Socket clientSocket = Util.connectToServerSocket("miner", 8000);
		ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
		
		// Transmit the updated blocks to the miner
		output.writeObject(updatedBlocks);
		
		// Close the connection
		clientSocket.close();
		System.out.println("Transmitted updated blocks");
		
		// Write the updated blocks to the database
		for (Block b : updatedBlocks.values()) {
			db.put(b.getBlockId(), Util.serialize(b));
		}
		
		// Clear the list of updated blocks
		updatedBlocks.clear();
		updatedBlocksLock.unlock();
	}
}
