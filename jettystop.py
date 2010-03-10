from socket import *

s = socket(AF_INET, SOCK_STREAM)
s.connect(('127.0.0.1', 8079))
s.send('\r\n')
s.close()
