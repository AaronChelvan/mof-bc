import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class Block implements Serializable {

	private static final long serialVersionUID = 1L;
	private ArrayList<Transaction> transactionList;
	private byte[] blockId;
	private byte[] prevBlockId;
	
	// Constructor
	public Block() {
		transactionList = new ArrayList<Transaction>(Config.numTransactionsInBlock);
		blockId = null;
		prevBlockId = null;
	}
	
	// Get the block ID
	public byte[] getBlockId() {
		return blockId;
	}
	
	// Get the previous block ID
	public byte[] getPrevBlockId() {
		return prevBlockId;
	}

	// Set the block ID
	public void setBlockId() {
		// Compute a hash from all transaction IDs and prev transaction IDs in this block
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		md.update(prevBlockId);
		for (Transaction t: transactionList) {
			md.update(t.getTid());
			md.update(t.getPrevTid());
		}
		
		blockId = md.digest();
	}
	
	// Set the block ID
	public void setBlockId(byte[] id) {
		blockId = id;
	}
	
	// Set the transactions in the block
	public void setTransactions(ArrayList<Transaction> transactionList) {
		this.transactionList = transactionList;
	}
	
	// Set the previous block ID
	public void setPrevBlockId(byte[] id) {
		prevBlockId = id;
	}

	// Return whether or not the block is full
	public boolean isFull() {
		if (transactionList.size() == Config.numTransactionsInBlock) {
			return true;
		} else {
			return false;
		}
	}
	
	// Add a transaction to the block
	public void addTransaction(Transaction t) {
		transactionList.add(t);
	}
	
	// Get a list of transactions in the block
	public ArrayList<Transaction> getTransactions() {
		return transactionList;
	}

}
