package com.deltapunkt.secproxy.interfaces;

import java.nio.channels.SocketChannel;

/*
 * correponds to a SelectionKey.OP_CONNECT event
 */
public interface ConnectHandler {
	void onConnect(SocketChannel sc);
}
