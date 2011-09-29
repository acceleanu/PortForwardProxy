package com.deltapunkt.secproxy.interfaces;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import com.deltapunkt.secproxy.Message;

public interface Reactor {
	void start();

	void registerAcceptor(SocketAddress sa, AcceptHandler pf);

	void registerConnector(SocketAddress targetAddress, ConnectHandler ch);

	void registerChannelHandler(SocketChannel sc, MessageConsumer ch);
	
	void makeChannelReadable(SocketChannel clientChannel);

	void makeChannelWritable(SocketChannel targetChannel);

	void sendMessage(Message message);

	void stop();
}
