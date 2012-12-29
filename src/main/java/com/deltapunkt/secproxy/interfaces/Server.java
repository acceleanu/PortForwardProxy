package com.deltapunkt.secproxy.interfaces;

import java.nio.channels.SocketChannel;

public interface Server {
	void start();
	void stop();
	void handleClient(SocketChannel clientChannel);
}
