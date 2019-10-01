import sys, os, plyvel

if len(sys.argv) != 2:
	print("Wrong number of args")
	exit()

if sys.argv[1] == "before":
	os.system("cp -a blockchain_copies/node_blockchain/ analysis/before/")
elif sys.argv[1] == "after":
	os.system("cp -a blockchain_copies/node_blockchain/ analysis/after/")
#elif sys.argv[1] == "before2":
#	os.system("cp -a blockchain_copies/node_blockchain/ analysis/before2/")
#elif sys.argv[1] == "after2":
#	os.system("cp -a blockchain_copies/node_blockchain/ analysis/after2/")
elif sys.argv[1] == "clear":
	os.system("rm -rf analysis/after/*")
	os.system("rm -rf analysis/before/*")
	os.system("sudo rm -rf blockchain_copies/*")
elif sys.argv[1] == "compare":
	db_list = ["./analysis/before/node_blockchain", "./analysis/after/node_blockchain"]
	#db_list = ["./analysis/before/node_blockchain", "./analysis/after/node_blockchain", "./analysis/before2/node_blockchain", "./analysis/after2/node_blockchain"]

	for database in db_list:
		db = plyvel.DB(database)

		total = 0
		for key, val in db:
			#print(repr(val))
			total += len(val)
		
		print("Size = %d" % total)

		sizes = []
		for key, val in db:
			sizes.append(len(val))
		print("\n" + str(sizes) + ". Num blocks = " + str(len(sizes)) + "\n")	
else:
	print("Invalid arg")
