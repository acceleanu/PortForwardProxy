package com.deltapunkt.secproxy;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.junit.Test;

import com.deltapunkt.secproxy.interfaces.Client;
import com.deltapunkt.secproxy.interfaces.Server;

public class TestClient2Server {
	private static final int SERVER_PORT = 2223;
	
	@Test
	public void clientConnectsAndReceivesMessageFromServer() throws InterruptedException {
		final String serverMessageSentToClient = "This message is sent by the server immediately afer accepting a connection from a client!";
		SocketAddress serverAddress = new InetSocketAddress("localhost", SERVER_PORT);
		Server server = new EchoServer(serverAddress) {
			public void handleClient(SocketChannel clientChannel) {
				ByteBuffer buffer = ByteBuffer.wrap(serverMessageSentToClient
						.getBytes());
				try {
					clientChannel.write(buffer);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		server.start();

		Client client = new EchoClient();
		client.connect(serverAddress);
		byte[] byteArrayReceivedFromServer = client.receive();
		String messageReceivedFromServer = new String(
				byteArrayReceivedFromServer);
		assertEquals(serverMessageSentToClient, messageReceivedFromServer);
		
		client.disconnect();
		server.stop();
		Thread.sleep(300);
	}

	@Test
	public void clientSendsAndReceivesMessageFromServer() throws InterruptedException {
		final String messageSentToServer = "This is the message sent to the server with the expectation to receive it back!";
		SocketAddress serverAddress = new InetSocketAddress("localhost", SERVER_PORT);
		Server server = new EchoServer(serverAddress) {
			public void handleClient(SocketChannel clientChannel) {
				ByteBuffer buffer = ByteBuffer.allocate(8192);
				try {
					clientChannel.read(buffer);
					buffer.flip();
					clientChannel.write(buffer);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		server.start();

		Client client = new EchoClient();
		
		client.connect(serverAddress);
		client.send(messageSentToServer.getBytes());
		byte[] byteArrayReceivedFromServer = client.receive();
		String messageReceivedFromServer = new String(
				byteArrayReceivedFromServer);

		assertEquals(messageSentToServer, messageReceivedFromServer);
		
		client.disconnect();
		server.stop();
		Thread.sleep(300);
	}
}
