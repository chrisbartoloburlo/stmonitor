SERVER_HOST = '127.0.0.1'
SERVER_PORT = 1335
MAX_GET_REQUESTS = 10

import re, socket
import random
import string

MSG_AUTH_RE = re.compile('''^AUTH +(.+) +(.+)''')
MSG_GET_RE = re.compile('''^GET +(.+) +(.+)''')
MSG_RVK_RE = re.compile('''^RVK +(.+)''')


def serve(srv):
    while 1:
        print('[S] Waiting for new connections')
        (s, address) = srv.accept()
        print('[S] New connection from', address)
        handle_connection(s)
        print('[S] Closing connection')
        s.close()


def handle_connection(s):
    print('[S] Waiting for request')
    auth = False
    while (not auth):
        req = s.recv(1024).decode().strip()
        print('[S] Received: ' + req)
        m = MSG_AUTH_RE.match(req)
        if (m is not None):
            letters = string.ascii_letters
            token = ''.join(random.choice(letters) for i in range(20))
            reply = 'SUCC ' + token
            print('[S] Replying: ', reply)
            s.sendall(str.encode(reply + '\n'))
            auth = True
            getRequests = 0

            while(auth):
                req = s.recv(1024).decode().strip()
                print('[S] Received: ' + req)
                m_get = MSG_GET_RE.match(req)
                m_rvk = MSG_RVK_RE.match(req)
                if (m_get is not None):
                    if (getRequests < MAX_GET_REQUESTS):
                        reply = 'RES content'
                        print('[S] Replying: ', reply)
                        s.sendall(str.encode(reply + '\n'))
                        getRequests += 1
                    else:
                        reply = 'TIMEOUT'
                        print('[S] Replying: ', reply)
                        s.sendall(str.encode(reply + '\n'))
                        auth = False
                elif (m_rvk is not None):
                    auth = True
                    break
        else:
            print('[S] Invalid message')


if (__name__ == '__main__'):
    # SERVER_PORT = int(argv[1])
    print('[S] Auth server starting. Press Ctrl+C to quit')
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)  # Avoid TIME_WAIT
    srv.bind((SERVER_HOST, SERVER_PORT))
    print('[S] Listening on ', SERVER_HOST, SERVER_PORT)
    srv.listen(8)
    serve(srv)
    srv.close()