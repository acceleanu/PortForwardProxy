package com.deltapunkt.secproxy.interfaces;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/*
 * correponds to a SelectionKey.OP_READ event
 */
public interface ReadHandler {
	void onRead(SocketChannel sc) throws IOException;
}
