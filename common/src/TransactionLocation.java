import java.io.Serializable;

public class TransactionLocation implements Serializable {

	private static final long serialVersionUID = 1L;
	private byte[] blockID;
	private byte[] transactionID;
	
	public TransactionLocation(byte[] blockID, byte[] transactionID) {
		this.blockID = blockID;
		this.transactionID = transactionID;
	}
	
	public byte[] getBlockID() {
		return blockID;
	}
	
	public byte[] getTransactionID() {
		return transactionID;
	}

}
