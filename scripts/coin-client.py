CLIENT_HOST = '127.0.0.1'
CLIENT_PORT = 1330
BIAS = 0.5

import socket
from numpy import random
import sys

if __name__ == '__main__':
  mode = int(sys.argv[1])
  print('[C] Client started')
  s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  s.connect((CLIENT_HOST, CLIENT_PORT))
  if(mode==1):
    print("Control mode: enter t for tails, h for heads and q to quit")
    count = 0
    while (True):
      key = input("")
      if key == "h":
        count+=1
        print('[C] Sending: HEADS',count)
        s.sendall(str.encode('HEADS\n'))
      elif key == "t":
        count+=1
        print('[C] Sending: TAILS',count)
        s.sendall(str.encode('TAILS\n'))
      elif key == "q":
        print('[C] Terminating')
        s.close()
        break
  elif(mode==2):
    iter = int(sys.argv[2])
    print(f"Random mode: executing randomly for $iter iterations with bias $BIAS")
    count = 0
    while(True):
      count+=1
      rand = random.uniform(low=0.0, high=1.0)
      if(rand <= BIAS):
        print('[C] Sending: HEADS',count)
        s.sendall(str.encode('HEADS\n'))
      else:
        print('[C] Sending: TAILS',count)
        s.sendall(str.encode('TAILS\n'))
      if count == iter:
        s.close()
        break
