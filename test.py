import sys, os, plyvel

if len(sys.argv) != 2:
	print("Wrong number of args")
	exit()

if sys.argv[1] == "before":
	os.system("cp -a blockchain_copies/node_blockchain/ analysis/before/")
elif sys.argv[1] == "after":
	os.system("cp -a blockchain_copies/node_blockchain/ analysis/after/")
elif sys.argv[1] == "clear":
	os.system("rm -rf analysis/after/*")
	os.system("rm -rf analysis/before/*")
	os.system("sudo rm -rf blockchain_copies/*")
elif sys.argv[1] == "compare":
	
	db_before = plyvel.DB("./analysis/before/node_blockchain")
	db_after = plyvel.DB("./analysis/after/node_blockchain")
	
	total_before = 0
	for key, val in db_before:
		total_before += len(val)

	total_after = 0
	for key, val in db_after:
		total_after += len(val)
	
	print("Size before = %d; Size after = %d" % (total_before, total_after))

	before_sizes = []
	for key, val in db_before:
		#print(repr(key))
		#print("Size = %d" % len(val))
		before_sizes.append(len(val))
	print("\nBEFORE: " + str(before_sizes) + ". Num blocks = " + str(len(before_sizes)))

	after_sizes = []
	for key, val in db_after:
		#print(repr(key))
		#print("Size = %d" % len(val))
		after_sizes.append(len(val))
	print("\nAFTER: " + str(after_sizes) + ". Num blocks = " + str(len(after_sizes)))
else:
	print("Invalid arg")
