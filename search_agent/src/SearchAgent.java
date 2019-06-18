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
		int cleaningPeriod = 5; // (Seconds)
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
			// Receive Block objects from the miner
			Socket connectionSocket = agentSocket.accept();
			ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream());
			Block currentBlock = (Block) in.readObject();
			
			// Add the block to the blockchain
			db.put(bytes(currentBlock.getBlockId()), Util.blockToBytes(currentBlock));			
		}
	}
	
	// TODO - How to keep track of Remove and Summary transactions that have already
	// been found in previous cleaning periods?
	// This function gets executed at regular intervals
	private static void searchBlockchain() throws ClassNotFoundException, IOException {
		System.out.println("Searching the blockchain");
		ArrayList<TransactionLocation> foundRemoveTransactions = new ArrayList<TransactionLocation>();
		ArrayList<TransactionLocation> foundSummaryTransactions = new ArrayList<TransactionLocation>();
		
		// Traverse the blockchain and look for Remove and Summary transactions
		DBIterator iterator = db.iterator();
		for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
			String blockHash = new String(iterator.peekNext().getKey());
			Block blockContents = Util.bytesToBlock(iterator.peekNext().getValue());
			
			for (Transaction t: blockContents.getTransactions()) {
				if (t.getType() == TransactionType.Remove) {
					foundRemoveTransactions.add(new TransactionLocation(blockHash, t.getTid()));
				} else if (t.getType() == TransactionType.Summary) {
					foundSummaryTransactions.add(new TransactionLocation(blockHash, t.getTid()));
				}
			}
		}
		
		// TODO - send foundRemoveTransactions to the Service Agent
		// TODO - send foundSummaryTransactions to the Summary Manager Agent
	}

}
