import java.util.ArrayList;

public class Block {
	private static final int maxNumTransactions = 1500; // Maximum number of transactions in a block
	private ArrayList<Transaction> transactionList;
	private String blockId;
	private String prevBlockId;
	
	public Block() {
		transactionList = new ArrayList<Transaction>();
		blockId = "";
		prevBlockId = "";
	}
	
	public void addTransaction(Transaction t) {
		transactionList.add(t);
	}
	
}
