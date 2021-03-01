SERVER_HOST = '127.0.0.1'
SERVER_PORT = 1335

import re, socket

MSG_AUTH_RE = re.compile('''^AUTH +(.+) +(.+)''')

def serve(srv):
    while 1:
        print('[S] Waiting for new connections')
        (s, address) = srv.accept()
        print('[S] New connection from',address)
        handle_connection(s)
        print('[S] Closing connection')
        s.close()

def handle_connection(s):
    print('[S] Waiting for request')
    req = s.recv(1024).decode().strip()
    print('[S] Received: '+req)
    m = MSG_AUTH_RE.match(req)
    if (m is not None):
        reply = 'FAIL 1'
        print('[S] Replying: ', reply)
        s.sendall(str.encode(reply+'\n'))
    else: # No message regex was matched
        print('[S] Invalid message')

    req = s.recv(1024).decode().strip()
    print('[S] Received: '+req)
    m = MSG_AUTH_RE.match(req)
    if (m is not None):
        reply = 'SUCC 123'
        print('[S] Replying: ', reply)
        s.sendall(str.encode(reply+'\n'))
    else:
        print('[S] Invalid message')


if (__name__ == '__main__'):
    # SERVER_PORT = int(argv[1])
    print('[S] Auth server starting. Press Ctrl+C to quit')
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) # Avoid TIME_WAIT
    srv.bind((SERVER_HOST, SERVER_PORT))
    print('[S] Listening on ',SERVER_HOST, SERVER_PORT)
    srv.listen(8)
    serve(srv)
    srv.close()