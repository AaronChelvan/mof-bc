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
	private static int numBlocks;
	private static DB db;
	private static Options options;
	
	private static Block currentBlock;
	private static byte[] prevBlockId;
	private static byte[] currentBlockId;

	public static void main(String[] args) throws Exception {
		System.out.println("Miner is running");
		numBlocks = 0;
		
		// LevelDB setup
		options = new Options();
		options.createIfMissing(true);
		
		// Blockchain configuration
		currentBlock = new Block();
		prevBlockId = ".".getBytes();
		currentBlockId = new byte[0];
		
		// Socket setup
		ServerSocket minerSocket = new ServerSocket(8000);
		
		while (true) {
			Socket connectionSocket = minerSocket.accept();
			
			// Identify who connected to the socket
			if (Util.socketClientName(connectionSocket).equals("node")) { // Received a transaction
				//System.out.println("Node connected to miner");
				processTransaction(connectionSocket);
			} else if (Util.socketClientName(connectionSocket).equals("service_agent")) { // Received updated blocks
				System.out.println("Service agent connected to miner");
				updateBlockchain(connectionSocket);
			} else if (Util.socketClientName(connectionSocket).equals("summary_manager_agent")) { // Received updated blocks
				System.out.println("Summary manager agent connected to miner");
				updateBlockchain(connectionSocket);
			}
		}
	}
	
	// Handles when a transaction is received from a node
	private static void processTransaction(Socket connectionSocket) throws DBException, IOException, ClassNotFoundException, InterruptedException {
		ObjectInputStream in = new ObjectInputStream(connectionSocket.getInputStream());
		
		//System.out.println("processing transaction");
		Transaction t = (Transaction) in.readObject();
		if (t.getType() == TransactionType.Standard) {
			System.out.println("Received std tx = " + DatatypeConverter.printHexBinary(t.getTid()));
		} else if (t.getType() == TransactionType.Remove) {
			System.out.println("Received rmv tx = " + DatatypeConverter.printHexBinary(t.getTid()));
		} else if (t.getType() == TransactionType.Summary) {
			System.out.println("Received smy tx = " + DatatypeConverter.printHexBinary(t.getTid()));
		} else { // Something went wrong
			System.exit(0);
		}
		
		// Add the transaction to the block
		currentBlock.addTransaction(t);
		
		if (currentBlock.isFull()) {
			System.out.println("Block filled");
			
			// Set the prevBlockId and blockId fields
			currentBlock.setPrevBlockId(prevBlockId);
			currentBlock.setBlockId();
			currentBlockId = currentBlock.getBlockId();
			
			// Add the block to the blockchain
			db = factory.open(new File("blockchain"), options);
			db.put(currentBlockId, Util.serialize(currentBlock));
			db.close();
			numBlocks++;
			
			// Transmit the block to all nodes and agents
			List<String> blockRecipients = Arrays.asList("service_agent", "node1", "search_agent", "summary_manager_agent");
			transmitBlock(currentBlock, blockRecipients);
			
			// Wait indefinitely once the blockchain has been filled
			if (Config.mode == 0) {
				if (numBlocks >= Config.maxBlockchainSize) {
					System.out.println("Blockchain is full");
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
		
		HashMap<String, Block> updatedBlocks = (HashMap<String, Block>) in.readObject();
		db = factory.open(new File("blockchain"), options);
		for (Block b: updatedBlocks.values()) {
			// Add the updated block to the blockchain
			db.put(b.getBlockId(), Util.serialize(b));
			System.out.println("Received updated block = " + DatatypeConverter.printHexBinary(b.getBlockId()));
			
			// Transmit this updated block to the nodes
			ArrayList<String> recipients = new ArrayList<String>(); 
			recipients.add("node1");
			transmitBlock(b, recipients);
		}
		db.close();
		
		System.out.println("Miner transmitted updated blocks");
	}
	
	// Given a block and a list of recipients, transmit the block to all recipients
	private static void transmitBlock(Block currentBlock, List<String> recipients) throws UnknownHostException, IOException {
		System.out.println("Transmitting block = " + DatatypeConverter.printHexBinary(currentBlock.getBlockId()));
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
