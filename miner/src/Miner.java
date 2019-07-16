import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.DatatypeConverter;

import org.iq80.leveldb.*;
import static org.fusesource.leveldbjni.JniDBFactory.*;
import java.io.*;

public class Miner {
	private static int maxBlockchainSize;
	private static int numBlocks;
	private static DB db;
	
	private static Block currentBlock;
	private static byte[] prevTransactionId;
	private static byte[] prevBlockId;
	private static byte[] currentBlockId;
	
	private static int mode;

	public static void main(String[] args) throws Exception {
		mode = 0; // 0 == create blockchain. 1 == remove transactions.
		
		System.out.println("Miner is running");
		maxBlockchainSize = 10;
		numBlocks = 0;
		
		// LevelDB setup
		Options options = new Options();
		options.createIfMissing(true);
		db = factory.open(new File("blockchain"), options);
		
		// Blockchain configuration
		currentBlock = new Block();
		prevTransactionId = new byte[0];
		prevBlockId = new byte[0];
		currentBlockId = new byte[0];
		
		// Socket setup
		ServerSocket minerSocket = new ServerSocket(8000);
		
		while (true) {
			Socket connectionSocket = minerSocket.accept();
			
			// Identify who connected to the socket
			if (Util.socketClientName(connectionSocket).equals("node")) { // Received a transaction
				//System.out.println("Node connected to miner");
				processTransaction(connectionSocket);
			} else if (Util.socketClientName(connectionSocket).equals("service_agent")) { // Received an updated block
				System.out.println("Service agent connected to miner");
				updateBlockchain(connectionSocket);
			}
		}
	}
	
	// Handles when a transaction is received from a node
	private static void processTransaction(Socket connectionSocket) throws DBException, IOException, ClassNotFoundException, InterruptedException {
		ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream());
		
		//System.out.println("processing transaction");
		Transaction t = (Transaction) in.readObject();
		System.out.println("Received transaction = " + DatatypeConverter.printHexBinary(t.getTid()));
		
		// Add the transaction to the block
		currentBlock.addTransaction(t);
		
		if (currentBlock.isFull()) {
			System.out.println("Block filled");
			
			// Set the prevBlockId and blockId fields
			currentBlock.setPrevBlockId(prevBlockId);
			currentBlock.setBlockId();
			currentBlockId = currentBlock.getBlockId();
			
			// Add the block to the blockchain
			db.put(currentBlockId, Util.serialize(currentBlock));
			numBlocks++;
			
			// Transmit the block to all nodes and agents
			List<String> blockRecipients = Arrays.asList("service_agent", "node1", "search_agent");
			transmitBlock(currentBlock, blockRecipients);
			
			// Checks if a key does not exist
			/*if (db.get(bytes("qwioeiugryf")) == null) {
				System.out.println("key does not exist");
			}*/
			
			// Wait indefinitely once the blockchain has been filled
			if (mode == 0) {
				if (numBlocks >= maxBlockchainSize) {
					System.out.println("Blockchain is full");
					db.close();
					while (true) {
						TimeUnit.MINUTES.sleep(1);
					}
				}
			}
			
			// Start adding transactions to a new block
			currentBlock = new Block();
			prevBlockId = currentBlockId;
		}
	}
	
	// Handles when an updated block (contains transactions that have been removed or summarised)
	// is received from an agent
	private static void updateBlockchain(Socket connectionSocket) throws ClassNotFoundException, IOException{
		ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream());
		
		HashMap<byte[], Block> updatedBlocks = (HashMap<byte[], Block>) in.readObject();
		for (byte[] blockId: updatedBlocks.keySet()) {
			db.put(blockId, Util.serialize(updatedBlocks.get(blockId)));
			
			// Transmit this updated block to the nodes and agents
			// (except for the agent that sent this updated block to the miner)
			ArrayList<String> recipients = new ArrayList<String>(); 
			//recipients.add("service_agent");
			recipients.add("node1");
			//recipients.add("node2");
			//recipients.add("search_agent");
			recipients.remove(Util.socketClientName(connectionSocket));
			
			transmitBlock(updatedBlocks.get(blockId), recipients);
		}
		
		System.out.println("Miner transmitted updated blocks");
	}
	
	// Given a block and a list of recipients, transmit the block to all recipients
	private static void transmitBlock(Block currentBlock, List<String> recipients) throws UnknownHostException, IOException {
		for (String s: recipients) {
			// Open a connection
			Socket clientSocket = Util.connectToServerSocket(s, 8000);
			
			// Transmit the block
			ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
			output.writeObject(currentBlock);
			
			// Close the connection
			clientSocket.close();
		}
	}

}
