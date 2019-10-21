
public class Config {
	// 0 == create blockchain
	// 1 == remove transactions
	// 2 == summarize transactions
	// 3 == convert DB to JSON
	// 4 == convert DB from JSON
	// 5 == convert DB to CSV
	// 6 == convert DB from CSV
	public static int mode = 0;
	
	public static final int dataSize = 100; // Size of data in a standard transaction
	public static final double removalPercentage = 1.0; // Percentage of transactions that should be removed. 0.25 == 25%
	public static final int numTransactionsInSummary = 5; // The number of transactions in a single summary transaction
	
	// Should the service agent and summary manager agent transmit multiple changes at once at the end of the cleaning period interval?
	public static boolean enableCleaningPeriod = true;
	
	public static final int cleaningPeriod = 60; // The length of the cleaning period (seconds)
	public static final int numTransactionsInBlock = 20; // Maximum number of transactions in a block
	public static final int maxBlockchainSize = 100; // Maximum number of blocks in a blockchain
}
