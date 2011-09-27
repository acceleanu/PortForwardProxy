package com.deltapunkt.secproxy;

import java.nio.channels.SocketChannel;

import com.deltapunkt.secproxy.interfaces.MessageConsumer;
import com.deltapunkt.secproxy.interfaces.ReadHandler;
import com.deltapunkt.secproxy.interfaces.WriteHandler;

public interface ConnectionManager extends WriteHandler, ReadHandler {
	void addConnection(SocketChannel sc, MessageConsumer messageConsumer);

	void sendMessage(Message message);
}
