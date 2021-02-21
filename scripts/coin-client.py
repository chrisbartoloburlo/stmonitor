CLIENT_HOST = '127.0.0.1'
CLIENT_PORT = 1330
BIAS = 0.5

import socket
from numpy import random

if __name__ == '__main__':
  print('[C] Client started')
  s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  s.connect((CLIENT_HOST, CLIENT_PORT))
  count = 0
  while (True):
    count+=1
    key = input("")
    if key == "h":
      print('[C] Sending: HEADS',count)
      s.sendall(str.encode('HEADS\n'))
    elif key == "t":
      print('[C] Sending: TAILS',count)
      s.sendall(str.encode('TAILS\n'))
    # if count == 100:
    #   s.close()
    #   break