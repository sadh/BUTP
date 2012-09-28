BUTP is a TCP like UDP based socket implemented with flow cortrol, error control and congestion control<br />
====

<br /><br />Usage:<br />
//For server<br />
BUTPSocket socket = new BUTPSocket(9001);<br />
socket.accept();<br />
socket.receive(bytearray);<br />
socket.close();<br /><br />

//For client<br />
BUTPSocket socket = new BUTPSocket('server IP',server port):<br />
socket.connect();<br />
socket.send(bytearray);<br />
socket.close();<br />