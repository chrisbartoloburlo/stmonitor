#!/usr/bin/env python

CLIENT_HOST = '127.0.0.1'
CLIENT_PORT = 1330

import re, socket

if (__name__ == '__main__'):
    print '[C] Client started'
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((CLIENT_HOST,CLIENT_PORT))

    print '[C] Sending: LOGIN Adrian Francalanza 123token'
    s.sendall('LOGIN Adrian Francalanza 123token\n')
    s.recv(5) # waits for ACK

    resp = s.recv(1024)
    print '[C] Received: ', resp
    s.sendall('ACK\n')

    print '[C] Sending: LOGIN Chris Bartolo 123token'
    s.sendall('LOGIN Chris Bartolo 123token\n')
    s.recv(5) # waits for ACK

    resp = s.recv(1024)
    print '[C] Received: ', resp
    s.sendall('ACK\n')
    s.close()
