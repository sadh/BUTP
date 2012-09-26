BUTP is a TCP like UDP based socket implemented with flow cortrol, error control and congestion control
====

Usage:
//For server
BUTPSocket socket = new BUTPSocket(9001);
socket.accept();
socket.receive(bytearray);
socket.close();

//For client
BUTPSocket socket = new BUTPSocket('server IP',server port):
socket.connect();
socket.send(bytearray);
socket.close();