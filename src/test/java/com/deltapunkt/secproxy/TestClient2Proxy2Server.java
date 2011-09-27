package com.deltapunkt.secproxy;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.junit.Test;

import com.deltapunkt.secproxy.interfaces.Client;
import com.deltapunkt.secproxy.interfaces.ProxyFactory;
import com.deltapunkt.secproxy.interfaces.Reactor;
import com.deltapunkt.secproxy.interfaces.Server;

public class TestClient2Proxy2Server {
	private static final int PROXY_PORT = 2222;
	private static final int SERVER_PORT = 2223;

	@Test
	public void clientConnectsToTheProxyAndReceivesMessage() throws InterruptedException{
		//start server
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
		//start proxy
		TaskScheduler taskScheduler = new TaskScheduler(10);
		ConnectionManager connectionHandler = new PeerConnectionManager(taskScheduler);
		
		Reactor reactor = new DefaultReactor(connectionHandler);
		reactor.start();
		SocketAddress targetAddress = new InetSocketAddress("localhost", SERVER_PORT);
		ProxyFactory proxyFactory = new PortForwardProxyFactory(targetAddress);
		SocketAddress proxyAddress = new InetSocketAddress("localhost", PROXY_PORT);
		reactor.addPortListener(proxyAddress, proxyFactory);
		//connect client
		Client client = new EchoClient();
		client.connect(proxyAddress);
		byte[] byteArrayReceivedFromServer = client.receive();
		String messageReceivedFromServer = new String(
				byteArrayReceivedFromServer);
		System.out.println(serverMessageSentToClient + " vs " + messageReceivedFromServer);
		assertEquals(serverMessageSentToClient, messageReceivedFromServer);
		
		client.disconnect();
		reactor.stop();
		server.stop();
		
		Thread.sleep(300);
	}
}
