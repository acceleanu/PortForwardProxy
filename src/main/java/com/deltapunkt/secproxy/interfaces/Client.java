package com.deltapunkt.secproxy.interfaces;

import java.net.SocketAddress;

public interface Client {
	void connect(SocketAddress serverAddress);
	byte[] receive();
	byte[] receive(int count);
	void send(byte[] message);
	void disconnect();
}
