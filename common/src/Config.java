
public class Config {
	// 0 == create blockchain
	// 1 == remove transactions
	// 2 == summarize transactions
	// 3 == convert DB to JSON
	// 4 == convert DB from JSON
	public static int mode = 3;
	
	public static final int dataSize = 100; // Size of data in a standard transaction
	public static final double removalPercentage = 1.0; // Percentage of transactions that should be removed. 0.25 == 25%
	public static final int numTransactionsInSummary = 5; // The number of transactions in a single summary transaction
	public static final int cleaningPeriod = 5; // The length of the cleaning period (seconds)
	public static final int numTransactionsInBlock = 5; // Maximum number of transactions in a block
	public static final int maxBlockchainSize = 5; // Maximum number of blocks in a blockchain
	
	// Set to true if the nodes should serialize blocks in JSON format instead of using Java's object serialization
	public static boolean jsonSerialization = true;
}
