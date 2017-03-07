"""Matrix multiply speed check.

This module call matrix multiplication code written by C to multiple matrixA and 
matrixB using the naivestrategy and the blocked strategy for each given block 
size. And create a graph `graph.png' that shows the results with matrix size on 
the x-axis and time on the y-axis to compare the speed of the two aglo.

# param 1: file name 
# param 2-N: different block sizes

"""

import sys
import ctypes
import time
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import timeit
from subprocess import call
import subprocess

__version__ = '0.0.1'
__author__ = 'WangBo'

file_name = sys.argv[1]
block_sizes = [int(block_size) for block_size in sys.argv[2:]]

time_spend_list_naive = []
time_spend_list_block = []

print "This script executes matrix multiplication for the matrixes in file '"+file_name+"' using the naive strategy and the blocked strategy for block sizes: "+", ".join(sys.argv[2:])
print "The result of this script is a graph 'graph.png' in the current directory that shows the results with matrix size on the x-axis and time on the y-axis. Create one line for each block size and one line for the naive strategy."

def matrixm(file_name):
    with open(file_name, 'r') as f:
        for line in f:
            #print line
            args = line.split(" ")
            n = int(args[0])
            matrixA = args[1]
            matrixB = args[2]
            time_start1 = time.time()
            call(["./matrix_multiplication", "naive", str(n), matrixA, matrixB])
            time_stop1 = time.time()
            time_spend1 = time_stop1 - time_start1
            time_spend_list_naive.append(time_spend1)
            
            time_start2 = time.time()
            call(["./matrix_multiplication", "blocked", str(n), matrixA, matrixB])
            time_stop2 = time.time()
            time_spend2 = time_stop2 - time_start2
            time_spend_list_block.append(time_spend2)
            
if __name__ == "__main__":
    matrixm(file_name)
    plt.plot(block_sizes, time_spend_list_naive, 'r--', label = "naive")  
    plt.plot(block_sizes, time_spend_list_naive, 'b--', label = "blocked")
    plt.legend(loc='best')
    plt.xlabel('Matrix size')
    plt.ylabel('time used (sec)')
    plt.title('Matrix mul speed check')
    plt.savefig("graph.png")
    print "Exe finished!"

