# mof-bc

## Testing procedure
1. Configure the network to only create standard transactions (set mode to 0 in the node and miner)
2. Run the MOF-BC network until the blockchain has been filled
3. Use docker-compose volumes to save a copy of the miner's, node's, and service agent's blockchains to the host machine
4. Set the mode to 1 in the node and miner
5. Run the network again
6. Compare the miner's blockchain sizes before and after 
