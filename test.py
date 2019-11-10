#!/usr/bin/python
import sys, os, plyvel

if len(sys.argv) != 2:
	print("Wrong number of args")
	exit()

if sys.argv[1] == "clear": # Delete the blockchains and their copies
	os.system("rm -rf analysis/*")
	os.system("sudo rm -rf blockchain_copies/*")
elif sys.argv[1] == "compare": # Compare all of the blockchains in the "analysis" folder
	db_list = os.listdir("./analysis/")

	for database in db_list:
		print(database)
		db = plyvel.DB("./analysis/" + database)

		total = 0
		for key, val in db:
			#print(repr(val))
			total += len(val)
		
		print("Size = %d" % total)

		sizes = []
		for key, val in db:
			sizes.append(len(val))
		print("\n" + str(sizes) + ". Num blocks = " + str(len(sizes)) + "\n")
else: # Copy the node's blockchain to the analysis folder using the given argument as the folder name
	folder = sys.argv[1]
	os.system("cp -a blockchain_copies/node_blockchain/ analysis/%s/" % (folder))
