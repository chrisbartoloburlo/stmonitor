#!/usr/bin/env python

CLIENT_HOST = '127.0.0.1'
CLIENT_PORT = 1330

import re, socket

MSG_SUCC_RE = re.compile('''^SUCC +(.+)''')
MSG_FAIL_RE = re.compile('''^FAIL +(.+)''')
MSG_RES_RE = re.compile('''^RES +(.+)''')
MSG_TIMEOUT = re.compile('''^TIMEOUT''')

if (__name__ == '__main__'):
    print('[C] Client started')
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((CLIENT_HOST, CLIENT_PORT))

    req = 'AUTH Bob ro5'
    print(f'[C] Sending: {req}')
    s.sendall(str.encode(req + '\n'))
    rsp = s.recv(32).decode().strip()
    print('[C] Received:', rsp)

    m = MSG_SUCC_RE.match(rsp)
    if (m is not None):
        tok = m.group(1)
        req = 'GET whatever '+tok
        print(f'[C] Sending: {req}')
        s.sendall(str.encode(req + '\n'))

        rsp = s.recv(32).decode().strip()
        print('[C] Received:', rsp)

        req = 'RVK ' + tok
        print(f'[C] Sending: {req}')
        s.sendall(str.encode(req + '\n'))
