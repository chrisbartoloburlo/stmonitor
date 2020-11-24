#!/usr/bin/env python

CLIENT_HOST = '127.0.0.1'
CLIENT_PORT = 1330

import re, socket

if (__name__ == '__main__'):
    print '[C] Client started'
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((CLIENT_HOST,CLIENT_PORT))

    print '[C] Sending: AUTH%Bob%ro5'
    s.sendall('AUTH%Bob%ro5\n')
    s.recv(5) # waits for ACK

    resp = s.recv(1024)
    print '[C] Received: ', resp
    s.sendall('ACK\n')

    print '[C] Sending: AUTH%Chris%227'
    s.sendall('AUTH%Chris%227\n')
    s.recv(5) # waits for ACK

    resp = s.recv(1024)
    print '[C] Received: ', resp
    s.sendall('ACK\n')
    s.close()
