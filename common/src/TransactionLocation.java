
public class TransactionLocation {
	
	private String blockID;
	private String transactionID;
	
	public TransactionLocation(String blockID, String transactionID) {
		this.blockID = blockID;
		this.transactionID = transactionID;
	}
	
	public String getBlockID() {
		return blockID;
	}
	
	public String getTransactionID() {
		return transactionID;
	}

}
