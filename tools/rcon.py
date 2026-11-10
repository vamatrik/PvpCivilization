import socket, struct, sys
def pkt(i, t, s): b = s.encode()+b'\x00\x00'; return struct.pack('<iii', len(b)+8, i, t)+b
s = socket.create_connection(('127.0.0.1', 25575)); s.send(pkt(1,3,'test')); s.recv(4096)
for c in sys.argv[1:]:
    s.send(pkt(2,2,c)); data = s.recv(65536); print(data[12:-2].decode(errors='replace'))
