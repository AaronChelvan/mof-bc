import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.iq80.leveldb.*;
import static org.fusesource.leveldbjni.JniDBFactory.*;
import java.io.*;

public class Miner {
	// Convert a Block object into a byte[] for storage in the DB
	// https://stackoverflow.com/a/30968827
	private static byte[] convertToBytes(Block b) throws IOException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(bos)) {
			out.writeObject(b);
			return bos.toByteArray();
		}
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Miner is running");
		int maxBlockchainSize = 100;
		
		// LevelDB setup
		Options options = new Options();
		options.createIfMissing(true);
		
		ArrayList<Block> blockchain = new ArrayList<Block>();
		Block currentBlock = new Block();
		String prevTransactionId = "";
		String prevBlockId = "";
		String currentBlockId = "";
		
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
				
				// Set the prevBlockId and blockId fields
				currentBlock.setPrevBlockId(prevBlockId);
				currentBlock.setBlockId();
				currentBlockId = currentBlock.getBlockId();
				
				// Add the block to the blockchain
				DB db = factory.open(new File("blockchain"), options);
				db.put(bytes(currentBlockId), convertToBytes(currentBlock));
				db.close();
				
				// Stop running once a certain amount of blocks have been added to the blockchain
				if (blockchain.size() >= maxBlockchainSize) {
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
