
public class Config {
	public static int mode = 1; // 0 == create blockchain. 1 == remove transactions.
	public static final int dataSize = 100; // Size of data in a standard transaction
	public static final double removalPercentage = 0.5; // Percentage of transactions that should be removed. 0.25 == 25%
	public static final int cleaningPeriod = 5; // (Seconds)
	public static final int maxNumTransactions = 100; // Maximum number of transactions in a block
}
