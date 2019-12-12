import socket
import matplotlib.pyplot as plt
from socket_station import SocketStation
import sys
import numpy as np

if len(sys.argv) != 3:
    print('Usage: {} <hostip> <hostport>'.format(sys.argv[0]))
    exit(-1)
host = sys.argv[1]
port = int(sys.argv[2])

s = socket.socket()
s.bind((host, port))

plt.ion()                       # Enable interactive mode
fig = plt.figure()              # Create figure
axes = fig.add_subplot(1, 1, 1) # Add subplot
axes.set_xlim(-100, 100)
axes.set_ylim(-100, 100)

xpositions = []
ypositions = []
graph, = plt.plot(xpositions, ypositions, 'g-')    # Plot blank data

s.listen(0)                 # No extra unaccepted connections allowed
c, addr = s.accept()
print('client connected from ', addr)
ss = SocketStation(c)
pointer_origin = np.asarray([10, 10])

i = 1
while True:
    try:
        msg = ss.receive()
        if msg == 'reset':
            xpositions = []
            ypositions = []
            graph.set_data(ypositions, xpositions)  # update data
            plt.draw()                              # Redraw
            plt.pause(1e-17)
            continue
        position = msg.split()
        x = float(position[0])
        y = float(position[1])
        angle = float(position[2])
        pointer = (np.asarray([np.cos(angle * np.pi / 180), np.sin(angle * np.pi / 180)]) * 20) + pointer_origin
        xpositions.append(x)
        ypositions.append(y)
        plt.annotate(s='', xy=(pointer), xytext=(pointer_origin), arrowprops=dict(arrowstyle='->'))
        graph.set_data(ypositions, xpositions)  # update data
        plt.draw()                              # Redraw
        plt.pause(1e-17)
        # print('Frame # = {}'.format(i), end='\r')
        i += 1
    except Exception as e:
        print(e)
        print('closing simulator')
        break

c.close()
