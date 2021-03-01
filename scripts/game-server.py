#!/usr/bin/env python

SERVER_HOST = '127.0.0.1'
SERVER_PORT = 1337

import re, socket

MSG_GUESS_RE = re.compile('''^GUESS +(.+)''')
MSG_QUIT_RE  = re.compile('''^QUIT''')

def serve(srv):
    while 1:
        print('[S] Waiting for new connections')
        (s, address) = srv.accept()
        sf = s.makefile('rw')
        print('[S] New connection from {0}'.format(address))
        handle_connection(sf)
        print('[S] Closing connection')
        sf.close(); s.close()

def handle_connection(sf):
    while 1:
        print('[S] Waiting for request')
        req = sf.readline().strip()
        print('[S] Received: "{0}"'.format(req))
        
        m = MSG_GUESS_RE.match(req)
        if (m is not None):
            if(int(m.group(1)) == 3):
                ans = 'CORRECT {0}'.format(3)
                print('[S] Answering: "{0}"'.format(ans))
                sf.write(ans); sf.write("\n"); sf.flush()
                break
            else:
                ans = 'INCORRECT'
                print('[S] Answering: "{0}"'.format(ans))
                sf.write(ans); sf.write("\n"); sf.flush()
                continue
        
        m = MSG_QUIT_RE.match(req)
        if (m is not None):
            print('[S] End of session')
            break
        
        # No message regex was matched
        print('[S] Invalid message')
        break

if (__name__ == '__main__'):
    print('[S] Game server starting. Press Ctrl+C to quit')
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) # Avoid TIME_WAIT
    srv.bind((SERVER_HOST, SERVER_PORT))
    print(f'[S] Listening on {SERVER_HOST}:{SERVER_PORT}')
    srv.listen(8)
    serve(srv)
    srv.close()
