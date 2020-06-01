#This python scripts takes a riscv executable file and a logging file of PC and prints the time used to execute each function
#If a function calls another function, the time is spent in the other function. So main will probably not be the one with the most execution time.

import sys
from intervaltree import Interval, IntervalTree 

import re

dumped = sys.argv[1]
logFile    = sys.argv[2]

tree = IntervalTree()

startInterval = 0
endInterval   = 0
intervalName  = ""

#Make itnervalTree for functions
with open(dumped) as f:
    funcMatch = "([0-9a-f]+) <(.*)>:"
    instMatch = "([0-9a-f]+):(.*)"

    for line in f.readlines():

        func = re.search(funcMatch,line)
        inst = re.search(instMatch,line)
        if func:
            #print(func.group(1))
            startInterval = int(func.group(1),16)
            intervalName  = func.group(2)
        elif inst:
            endInterval = int(inst.group(1),16)+1
        elif len(line) <= 1:
            #Save into intervaltree
            if startInterval != 0 and endInterval != 0:
                #print(startInterval,endInterval,intervalName)
                tree[startInterval:endInterval] = (intervalName,0)
                startInterval = 0
                endInterval = 0

#Update intervalTree with running time
with open(logFile) as f:
    match = "Key: 0x([0-9a-f]+), value:([0-9]+)"
    for line in f.readlines():
        matched = re.match(match,line)
        addr = int(matched.group(1),16) 
        number = int(matched.group(2))
        old = sorted(tree[addr])
        #print(old)
        if old:
            tree.remove(Interval(old[0].begin,old[0].end, old[0].data))
            #print(old[0].begin,old[0].end, old[0].data)
            tree[old[0].begin:old[0].end] = (old[0].data[0],old[0].data[1]+number)


funcList = []
for interval in tree:
    #print(interval)
    funcList.append((interval.data[0],interval.data[1]))

for func in sorted(funcList, key=lambda x: x[1]):
    print(func) 