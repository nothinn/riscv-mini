#Takes in a ihex file path, a dump.text path and an address offset.

import sys
import re

from codecs import decode
import struct

BPs = 16 #Number of bits for fraction

def int_to_bytes(n, length):
    return decode('%%0%dx' % (length << 1) % n, 'hex')[-length:]


def bin_to_float(b):
    """ Convert binary string to a float. """
    bf = int_to_bytes(int(b, 16), 4)  # 8 bytes needed for IEEE 754 binary64.
    return struct.unpack('>f', bf)[0]

def binfloat_to_binfixed(b):
    f = bin_to_float(b)

    print(f)

    val = round(f*2**BPs)

    print(val)

    if val >= 2**31-1:
        val = 2**31-1
        print("Clipped to max value")
    elif val <= -2**31:
        val = -2**31
        print("Clipped to min value")

    hexStr = "%0.8X" % val



    return hexStr



#binfloat_to_binfixed("40866666")

ihex = sys.argv[1]
dump = sys.argv[2]
offs = sys.argv[3]

pattern = 'Disassembly of section (.+):'

#test = "Disassembly of section .text.startup:"


with open(dump) as f:
    lines = f.readlines()



validSection = False


flws = []


for line in lines:

    #Check if new section
    result = re.match(pattern,line)
    if result:
        if ".text" in result.group(1):
            validSection = True
        else:
            validSection = False
    else:
        #If we are in a valid section, which is not a section name
        if validSection:
            #Match groups
            #(address)(instruction)(name of instruction)
            val = re.match("([0-9a-f]+):\t([0-9a-f]+) +\t([a-z]+)",line)

            if val:
                if "flw" in val.group(3):
                    print(line)
                    out = int(re.match(".*# ([0-9a-f]+) <.*",line).group(1),16)
                    flws.append(out)

#print(flws)

#Save each one as a set of byte addresses
flw_bytes = []
for flw in flws:
    addresses = []
    for i in range(4):
        addresses.append(flw + i + int(offs,16))
    
    flw_bytes.append(addresses)

#print(flw_bytes)



#Go through the hex file:
pattern = ":([0-9A-F][0-9A-F])([0-9A-F][0-9A-F][0-9A-F][0-9A-F])([0-9A-F][0-9A-F])([0-9A-F]*)([0-9A-F][0-9A-F])"

with open(ihex) as f:
    lines = f.readlines()


float_val = ""

floats = {} #address indexed

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

        for byteAddr in range(int(byteCount,16)): #For each byte in the line
            for flw in flw_bytes: #For each flw
                for i in flw: #For each byte of the flw
                    if i == addr+byteAddr:
                        float_val = data[byteAddr*2:byteAddr*2+2] + float_val
                        if len(float_val) == 8:
                            #print(float_val)
                            floats[i-3] = binfloat_to_binfixed(float_val)
                            float_val = ""

    if record == "04":
        #Extended linear address
        address = int(data,16) << 16

    


float_bytes = {}

for index in floats:
    print(index, floats[index])
    for i in range(4):
        float_bytes[index +i] = floats[index][8-i*2-2:8-i*2]


newlines = []
#Convert floats to fixed point
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

        changed = False
        for byteAddr in range(int(byteCount,16)): #For each byte in the line
            if byteAddr+addr in float_bytes:
                changed = True
                line = line[:9+byteAddr*2] + float_bytes[byteAddr+addr] + line[9+byteAddr*2+2:]
        

        if changed:
            tmp = line[1:-3] #Extract bytes
            summed = 0
            for i in range(int(len(tmp)/2)):
                summed += int(tmp[i*2:i*2+2],16)

            lsb = summed & 0xff #Only take the LSB

            checksum = (-lsb)& 0xff

            newlines.append(line[:-3] + "%0.2X\n" % checksum)
        else:
            newlines.append(line)

    elif record == "04":
        #Extended linear address
        address = int(data,16) << 16
        newlines.append(line)
    else:
        newlines.append(line)

with open(ihex + ".fixed", "w") as f:
    for line in newlines:
        f.write(line)