CLIENT_HOST = '127.0.0.1'
# CLIENT_PORT = 1330

import socket
from numpy import random
import sys

# USAGE: python3 game-client.py $PORT $p_guess $p_help $p_quit
if __name__ == '__main__':
    CLIENT_PORT = int(sys.argv[1])
    p_guess = float(sys.argv[2])
    p_help = float(sys.argv[3])
    p_quit = float(sys.argv[4])
    print('[C] Client started')
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((CLIENT_HOST, CLIENT_PORT))

    while (True):
        flip = random.uniform(0, 1)
        if flip <= p_guess:
            guess = random.randint(1, 100)
            print('[C] Sending: GUESS', guess)
            guess = 'GUESS ' + str(guess) + '\n'
            s.sendall(str.encode(guess))
        elif flip <= p_guess + p_help:
            print('[C] Sending: HELP')
            s.sendall(str.encode('HELP\n'))
        elif flip <= p_guess + p_help + p_quit:
            print('[C] Sending: QUIT')
            s.sendall(str.encode('QUIT\n'))
            s.close()
            break

        resp = s.recv(1024).decode()
        print('[C] Received:', resp)
