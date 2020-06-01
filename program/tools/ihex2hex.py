# This script converts an IHEX file to eigth HEX files, such that they can be used for memory loading.

import sys
import re

ihex = sys.argv[1]


path = ihex[:ihex.rfind(".")]

outfiles = [open(path + "_0.hex","w+"),
    open(path + "_1.hex","w+"),
    open(path + "_2.hex","w+"),
    open(path + "_3.hex","w+"),
    open(path + "_4.hex","w+"),
    open(path + "_5.hex","w+"),
    open(path + "_6.hex","w+"),
    open(path + "_7.hex","w+")]

indexed = 0

#Go through the hex file:
pattern = ":([0-9A-F][0-9A-F])([0-9A-F][0-9A-F][0-9A-F][0-9A-F])([0-9A-F][0-9A-F])([0-9A-F]*)([0-9A-F][0-9A-F])"

with open(ihex) as f:
    lines = f.readlines()

lastAddr = 0
lastBytes = 0

first = True

counter = 0

address = 0 #Base address
for line in lines:
    match = re.match(pattern,line)

    if not match:
        print("Line not matched")
        print(line)
        Exception("LINE DID NOT MATCH")

    byteCount    = match.group(1)
    addressLower = match.group(2)
    record       = match.group(3)
    data         = match.group(4)
    checksum     = match.group(5)

    if record == "00": #data
        addr = address + int(addressLower,16)

        data_split = [data[index : index + 2] for index in range(0,len(data),2)]

        #print(addr - lastAddr)
        #print(byteCount)
        #print(" ")
        if lastBytes + lastAddr != addr and not first:
            while lastBytes + lastAddr != addr: #Fill in zeroes in empty address ranges
                outfiles[indexed%8].write("00" + "\n")
                indexed = indexed + 1
                lastBytes += 1
            print("Skip in hex file:", lastBytes, lastAddr, addr)
        first = False
        lastBytes = int(byteCount,16)
        lastAddr = addr
        #print(data_split, addr, indexed, addr-indexed) 

        for dat in data_split:
            outfiles[indexed%8].write(dat + "\n")
            indexed = indexed + 1
            counter += 1


    if record == "04":
        #Extended linear address
        address = int(data,16) << 16


for file in outfiles:
    file.close()

print("Number of bytes without skips:", counter)
print("Number of bytes with skips   :", indexed)
