#!/usr/bin/env python

CLIENT_HOST = '127.0.0.1'
CLIENT_PORT = 1330

import re, socket

if (__name__ == '__main__'):
    print '[C] Client started'
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((CLIENT_HOST,CLIENT_PORT))
    s.sendall('LOGIN Adrian Francalanza 123token\n')
    resp = s.recv(1024)
    print '[C] Received: ', resp
    s.sendall('LOGIN Chris Bartolo 123token\n')
    resp = s.recv(1024)
    print '[C] Received: ', resp
    s.close()
