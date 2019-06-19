import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.iq80.leveldb.*;
import static org.fusesource.leveldbjni.JniDBFactory.*;
import java.io.*;

public class Miner {

	public static void main(String[] args) throws Exception {
		System.out.println("Miner is running");
		int maxBlockchainSize = 100;
		int numBlocks = 0;
		
		// LevelDB setup
		Options options = new Options();
		options.createIfMissing(true);
		DB db = factory.open(new File("blockchain"), options);
		
		// Blockchain configuration
		Block currentBlock = new Block();
		String prevTransactionId = "";
		String prevBlockId = "";
		String currentBlockId = "";
		
		// Socket setup
		ServerSocket minerSocket = new ServerSocket(8000);
		
		while (true) {
			Socket connectionSocket = minerSocket.accept();
			ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream());
			Transaction t = (Transaction) in.readObject();
			
			// Set the prevTid and tid fields in the transaction
			t.setPrevTid(prevTransactionId);
			t.setTid();
			prevTransactionId = t.getTid();
			
			// Add the transaction to the block
			currentBlock.addTransaction(t);
			if (currentBlock.isFull()) {
				System.out.println("Block filled");
				System.out.println(Util.socketClientName(connectionSocket));
				
				// Set the prevBlockId and blockId fields
				currentBlock.setPrevBlockId(prevBlockId);
				currentBlock.setBlockId();
				currentBlockId = currentBlock.getBlockId();
				
				// Add the block to the blockchain
				db.put(bytes(currentBlockId), Util.serialize(currentBlock));
				numBlocks++;
				
				// Checks if a key does not exist
				/*if (db.get(bytes("qwioeiugryf")) == null) {
					System.out.println("key does not exist");
				}*/
				
				// Stop running once a certain amount of blocks have been added to the blockchain
				if (numBlocks >= maxBlockchainSize) {
					minerSocket.close();
					return;
				}
				
				// Start adding transactions to a new block
				currentBlock = new Block();
				prevTransactionId = "";
				prevBlockId = currentBlockId;
			}
		}
	}

}
