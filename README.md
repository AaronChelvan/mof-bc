# mof-bc

## Overview
This is an implementation of a Memory-Optimized and Flexible Blockchain (MOF-BC), that supports a subset of the features described in the paper: https://arxiv.org/abs/1801.04416. The features of this implementation include:
* Transaction creation
* Transaction removal & summarization
* 3 agents: search agent, service agent, and summary manager agent
* Cleaning periods
* Multiple block serialization formats: Java's object serialization format, JSON, CSV, and a custom serialization algorithm

## Implementation
Separate programs have been written for nodes, miners, and each of the agents. Each node/miner/agent runs within its own Docker container. Within the main project directory, there are separate subdirectories for the node program, the miner program, and each of the agents. Within each of these subdirectories is the source code for that particular program as well as a Dockerfile for it. There is also a subdirectory named `common` which contains source code that is shared between all of the programs.

Docker Compose is used to run these containers together in a network. The Docker Compose configuration is in the `docker-compose.yml` file. This file allows environment variables to be passed into the containers. The environment variables should be declared in a file named `.env`.

The `key_generator` folder contains a program for generating public and private key pairs. This is not part of the MOF-BC network. It is only used for generating keys which can then be pasted into the `.env` file. Note that the keys in the `.env` file should be encoded in Base64 (which this key generator will do anyway).
 
LevelDB (https://github.com/google/leveldb) is a key-value storage library which has been used in this implementation for storing the blockchain. To store a block, the block's ID is used as the key, and the serialized block is stored as the value. To used LevelDB with Java, LevelDB JNI (https://github.com/fusesource/leveldbjni) is used by compiling the nodes/miners/agents with the LevelDB JNI JAR file.

The Jackson API (https://github.com/FasterXML/jackson) is used for serializing a block into a JSON string, as well as for deserializing the JSON string back into a block.

## Dependencies
* Install Docker (https://docs.docker.com/install/)
* Install Docker Compose (https://docs.docker.com/compose/install/)

## Usage
* Edit the `common/src/Config.java` file to configure the options of the MOF-BC network (See the <a href="#Configuration">Configuration</a> section for details)
* Build the containers using `docker-compose build`
* Run the containers using `docker-compose up`
* Use `Ctrl+C` to stop the containers

## Configuration
The configuration file (`common/src/Config.java`) has the following options that can be edited:
* **mode** - Determines the behaviour of a node. Possible values:
    * **0** - Create transactions until the blockchain is full.
    * **1** - Remove transactions until the amount of transactions specified by **removalPercentage** have been removed.
    * **2** - Summarize transactions until the amount of transactions specified by **removalPercentage** have been removed.
    * **3** - Convert the node's blockchain from Java's object serialization format into JSON.
    * **4** - Convert the node's blockchain from JSON back to Java's object serialization format.
    * **5** - Convert the node's blockchain from Java's object serialization format into CSV.
    * **6** - Convert the node's blockchain from CSV back to Java's object serialization format.
    * **7** - Convert the node's blockchain from Java's object serialization format into the custom serialization algorithm.
    * **8** - Convert the node's blockchain from the custom serialization algorithms back to Java's object serialization format.
* **dataSize** - If the mode is set to 0, any future standard transactions created by the node will contain this many bytes of data (this value excludes the size of any additional metadata that will be included in the transaction). 
* **removalPercentage** - If **mode** is set to 1 or 2, this value specifies what proportion of the transactions will be removed/summarised. Expressed as a floating point number between 0 and 1.
* **numTransactionsInSummary** - The number of transactions that will be summarized together into a single summary transaction.
* **cleaningPeriod** - The length of the cleaning period (seconds).
* **numTransactionsInBlock** - The maximum number of transactions a block can contain.
* **maxBlockchainSize** - The maximum number of blocks in a blockchain. 

## Testing procedure
1. Configure the network to only create standard transactions (set mode to 0 in the node and miner)
2. Run the MOF-BC network until the blockchain has been filled
3. Use docker-compose volumes to save a copy of the miner's, node's, and service agent's blockchains to the host machine
4. Set the mode to 1 in the node and miner
5. Run the network again
6. Compare the miner's blockchain sizes before and after 
