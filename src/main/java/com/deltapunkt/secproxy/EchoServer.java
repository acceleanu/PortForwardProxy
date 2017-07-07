package com.deltapunkt.secproxy;

import com.deltapunkt.secproxy.interfaces.Server;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EchoServer implements Server, Runnable {
	private final ExecutorService es;
	private final CountDownLatch sync;
	private final SocketAddress serverAddress;
	private ServerSocketChannel serverChannel;
	private boolean running;

	public EchoServer(SocketAddress serverAddress) {
		this.serverAddress = serverAddress;
		es = Executors.newSingleThreadExecutor();
		sync = new CountDownLatch(1);
	}

	public void start() {
		es.execute(this);
		try {
			sync.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			serverChannel = ServerSocketChannel.open();
			serverChannel.socket().bind(serverAddress);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			sync.countDown();
		}
		running = true;
		try {
			while (running) {
				SocketChannel clientChannel = serverChannel.accept();
				handleClient(clientChannel);
			}
		} catch (Exception e) {
			System.out.println("Exception type: " + e.getClass());
		} finally {
			try {
				System.out.println("Closing server channel");
				serverChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void handleClient(SocketChannel clientChannel) {
	}

	public void stop() {
		running = false;
		es.shutdownNow();
	}
}
