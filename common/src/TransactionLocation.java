import java.io.Serializable;

public class TransactionLocation implements Serializable {

	private static final long serialVersionUID = 1L;
	private byte[] blockID;
	private byte[] transactionID;
	private byte[] prevTransactionID;
	
	// Constructor
	public TransactionLocation(byte[] blockID, byte[] transactionID, byte[] prevTransactionID) {
		this.blockID = blockID;
		this.transactionID = transactionID;
		this.prevTransactionID = prevTransactionID;
	}
	
	// Get the block ID
	public byte[] getBlockID() {
		return blockID;
	}
	
	// Get the transaction ID
	public byte[] getTransactionID() {
		return transactionID;
	}
	
	// Get the previous transaction ID
	public byte[] getPrevTransactionID() {
		return prevTransactionID;
	}

}
