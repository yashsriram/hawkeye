import socket
import matplotlib.pyplot as plt
from socket_station import SocketStation

host = '192.168.1.10'
port = 8000

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
        xpositions.append(x)
        ypositions.append(y)
        graph.set_data(ypositions, xpositions)  # update data
        plt.draw()                              # Redraw
        plt.pause(1e-17)
        i += 1
    except Exception as e:
        print(e)
        print('closing simulator')
        break

c.close()
