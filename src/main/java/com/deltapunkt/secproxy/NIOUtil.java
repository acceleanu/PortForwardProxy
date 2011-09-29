package com.deltapunkt.secproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public final class NIOUtil
{
	public static ServerSocketChannel createServerChannel(SocketAddress address)
			throws IOException, SocketException
	{
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		ServerSocket serverSocket = serverChannel.socket();
		serverSocket.setReuseAddress(true);
		serverSocket.bind(address);

		return serverChannel;
	}

	public static SocketChannel createClientChannel() throws IOException
	{
		SocketChannel clientChannel = SocketChannel.open();
		clientChannel.configureBlocking(false);
		
		return clientChannel;
	}

}
