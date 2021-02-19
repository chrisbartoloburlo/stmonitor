SERVER_HOST = '127.0.0.1'
# SERVER_PORT = 1335

import re, socket
from numpy import random
from sys import argv

MSG_GUESS_RE = re.compile('''^GUESS +(.+)''')
MSG_HELP_RE = re.compile('''^HELP''')
MSG_QUIT_RE  = re.compile('''^QUIT''')

def serve(srv, p_correct, p_incorrect):
  while 1:
    print('[S] Waiting for new connections')
    (s, address) = srv.accept()
    # sf = s.makefile('rw')
    print('[S] New connection from',address)
    handle_connection(s, p_correct, p_incorrect)
    print('[S] Closing connection')
    s.close();

def handle_connection(s, p_correct, p_incorrect):
  while 1:
    print('[S] Waiting for request')
    req = s.recv(1024).decode().strip()
    print('[S] Received: '+req)
    flip = random.uniform(0, 1)
    m = MSG_GUESS_RE.match(req)
    if (m is not None):
      if flip <= p_correct:
        reply = 'CORRECT'
        print('[S] Replying: ', reply)
        s.sendall(str.encode(reply+'\n'))
        continue
      elif flip <= p_correct + p_incorrect:
        reply = 'INCORRECT'
        print('[S] Replying: ', reply)
        s.sendall(str.encode(reply+'\n'))
        continue

    m = MSG_HELP_RE.match(req)
    if (m is not None):
      reply = 'HINT Some information'
      print('[S] Replying: ', reply)
      s.sendall(str.encode(reply+'\n'))
      continue
    
    m = MSG_QUIT_RE.match(req)
    if (m is not None):
      print('[S] End of session')
      break
    
    # No message regex was matched
    print('[S] Invalid message')
    break

# USAGE: python3 game-server.py $PORT $p_correct $p_incorrect
if (__name__ == '__main__'):
  SERVER_PORT = int(argv[1])
  p_correct, p_incorrect = float(argv[2]),float(argv[3])
  print('[S] Game server starting. Press Ctrl+C to quit')
  srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) # Avoid TIME_WAIT
  srv.bind((SERVER_HOST, SERVER_PORT))
  print('[S] Listening on ',SERVER_HOST, SERVER_PORT)
  srv.listen(8)
  serve(srv, p_correct, p_incorrect)
  srv.close()
