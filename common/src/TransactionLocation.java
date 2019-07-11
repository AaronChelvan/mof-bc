import java.io.Serializable;

public class TransactionLocation implements Serializable {

	private static final long serialVersionUID = 1L;
	private byte[] blockID;
	private byte[] transactionID;
	private byte[] prevTransactionID;
	
	public TransactionLocation(byte[] blockID, byte[] transactionID, byte[] prevTransactionID) {
		this.blockID = blockID;
		this.transactionID = transactionID;
		this.prevTransactionID = prevTransactionID;
	}
	
	public byte[] getBlockID() {
		return blockID;
	}
	
	public byte[] getTransactionID() {
		return transactionID;
	}
	
	public byte[] getPrevTransactionID() {
		return prevTransactionID;
	}

}
