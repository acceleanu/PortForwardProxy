package com.deltapunkt.secproxy;

import com.deltapunkt.secproxy.interfaces.Client;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class EchoClient implements Client {
	private SocketChannel clientChannel;

	public void connect(SocketAddress serverAddress) {
		try {
			clientChannel = SocketChannel.open();
			clientChannel.connect(serverAddress);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public byte[] receive(int count) {
		ByteBuffer buffer = ByteBuffer.wrap(new byte[count]);
		try {
			clientChannel.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		buffer.flip();

		byte[] vret = new byte[buffer.limit()];

		buffer.get(vret);

		return vret;
	}
	
	public byte[] receive() {
		return receive(8192);
	}

	public void send(byte[] message) {
		ByteBuffer buffer = ByteBuffer.wrap(message);
		try {
			clientChannel.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void disconnect() {
		try {
			clientChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
