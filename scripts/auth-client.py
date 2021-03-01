#!/usr/bin/env python

CLIENT_HOST = '127.0.0.1'
CLIENT_PORT = 1330

import re, socket

if (__name__ == '__main__'):
    print('[C] Client started')
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((CLIENT_HOST,CLIENT_PORT))

    reply = 'AUTH Bob ro5'
    print(f'[C] Sending: {reply}')
    s.sendall(str.encode(reply+'\n'))
    rcv = s.recv(16).decode().strip()
    print('[C] Received:', rcv)

    reply = 'AUTH Chris 227'
    print(f'[C] Sending: {reply}')
    s.sendall(str.encode(reply+'\n'))
    rcv = s.recv(16).decode().strip()
    print('[C] Received:', rcv)
    s.close()
