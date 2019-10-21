# Time to remove all transactions: (100 transactions per block)

### REDO now that all extra prints are disabled ###
100 transactions
749412493ns
795064981ns

500 transactions
4532994996ns

1000 transactions
8128059210ns

2000 transactions
13350307576ns
13122857417ns

Explanation:
* Overhead from scanning the entire blockchain to get a list of all transactions?

# Transaction removal - 2000 transactions
200 blocks, 10 t/b

100 blocks, 20 t/b
11592495869ns
13169995151ns

50 blocks, 40 t/b

20 blocks, 100 t/b

2 blocks, 1000 t/b

Explanation:
* More blocks with fewer transactions each -> blocks get filled quicker -> blocks get written to DB more often
