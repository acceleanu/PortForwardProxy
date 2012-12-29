package com.deltapunkt.secproxy.interfaces;

import java.nio.channels.SocketChannel;

/*
 * correponds to a SelectionKey.OP_ACCEPT event
 */
public interface AcceptHandler {
	void onAccept(SocketChannel sc);
}
