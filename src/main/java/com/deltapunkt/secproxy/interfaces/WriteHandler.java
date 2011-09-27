package com.deltapunkt.secproxy.interfaces;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/*
 * correponds to a SelectionKey.OP_WRITE event
 */
public interface WriteHandler {
	boolean onWrite(SocketChannel sc) throws IOException;
}
