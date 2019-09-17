
public class Config {
	public static int mode = 2; // 0 == create blockchain. 1 == remove transactions.
	public static final int dataSize = 100; // Size of data in a standard transaction
	public static final double removalPercentage = 0.5; // Percentage of transactions that should be removed. 0.25 == 25%
	public static final int numTransactionsInSummary = 20; // The number of transactions in a single summary transaction
	public static final int cleaningPeriod = 5; // (Seconds)
	public static final int numTransactionsInBlock = 100; // Maximum number of transactions in a block
	public static final int maxBlockchainSize = 20; // Maximum number of blocks in a blockchain
}
