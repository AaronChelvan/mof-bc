# mof-bc

## Overview
This is an implementation of a Memory-Optimized and Flexible Blockchain (MOF-BC), that supports a subset of the features described in the paper: https://arxiv.org/abs/1801.04416. The features of this implementation include:
* Transaction creation
* Transaction removal & summarization
* 3 agents: search agent, service agent, and summary manager agent
* Cleaning periods
* No transaction aging
* No reward system

## Implementation
Separate programs have been written for nodes, miners, and each of the agents. Each node/miner/agent runs within its own Docker container. Within the main project directory, there are separate subdirectories for the node program, the miner program, and each of the agents. Within each of these subdirectories is the source code for that particular program as well as a Dockerfile for it. There is also a subdirectory named `common` which contains source code that is shared between all of the programs.

Docker Compose is used to run these containers together in a network. The Docker Compose configuration is in the `docker-compose.yml` file. This file allows environment variables to be passed into the containers. The environment variables should be declared in a file named `.env`.

The `key_generator` folder contains a program for generating public and private key pairs. This is not part of the MOF-BC network. It is only used for generating keys which can then be pasted into the `.env` file. Note that the keys in the `.env` file should be encoded in Base64 (which this key generator will handle anyway).

## Dependencies
* Install Docker (https://docs.docker.com/install/)
* Install Docker Compose (https://docs.docker.com/compose/install/)

## Usage
* Edit the `common/src/Config.java` file to configure the options of the MOF-BC network
* Build the containers using `docker-compose build`
* Run the containers using `docker-compose up`
* Use `Ctrl+C` to stop the containers

## Testing procedure
1. Configure the network to only create standard transactions (set mode to 0 in the node and miner)
2. Run the MOF-BC network until the blockchain has been filled
3. Use docker-compose volumes to save a copy of the miner's, node's, and service agent's blockchains to the host machine
4. Set the mode to 1 in the node and miner
5. Run the network again
6. Compare the miner's blockchain sizes before and after 
