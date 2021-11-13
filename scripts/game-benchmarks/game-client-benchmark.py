CLIENT_HOST = '127.0.0.1'
# CLIENT_PORT = 1330

import socket
from numpy import random
import sys
import time
import pandas as pd

# USAGE: python3 game-client.py $PORT $p_help $max_tries $path
if __name__ == '__main__':
    CLIENT_PORT = int(sys.argv[1])
    p_help = float(sys.argv[2])
    max_tries = float(sys.argv[3])
    path = sys.argv[4]
    print('[C] Client started')
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((CLIENT_HOST, CLIENT_PORT))
    tries = 0
    time_df = pd.DataFrame(columns=['try', 'guess_time(ns)', 'help_time(ns)'])

    while (True):
        flip = random.uniform(0, 1)
        start = 0
        if max_tries == tries:
            print('[C] Sending: QUIT')
            s.sendall(str.encode('QUIT\n'))
            s.close()
            break
        else:
            tries += 1
            if flip <= p_help:
                print('[C] Sending: HELP')
                start = time.process_time_ns()
                s.sendall(str.encode('HELP\n'))
                resp = s.recv(1024).decode()
                stop = time.process_time_ns()
                time_df = time_df.append({'try': tries, 'guess_time(ns)': 0, 'help_time(ns)': stop-start}, ignore_index=True)
            else:
                guess = random.randint(1, 100)
                print('[C] Sending: GUESS', guess)
                guess = 'GUESS ' + str(guess) + '\n'
                start = time.process_time_ns()
                s.sendall(str.encode(guess))
                resp = s.recv(1024).decode()
                stop = time.process_time_ns()
                time_df = time_df.append({'try': tries, 'guess_time(ns)': stop-start, 'help_time(ns)': 0}, ignore_index=True)
            print('[C] Received:', resp)

        time_df.to_csv(path)
