package com.deltapunkt.secproxy;

import com.deltapunkt.secproxy.interfaces.Proxy;
import com.deltapunkt.secproxy.interfaces.Reactor;

import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class PortForwardProxy implements Proxy {
	private final Reactor reactor;
	private final SocketChannel clientChannel;
	private final SocketChannel targetChannel;
	private final BlockingDeque<Message> queue;
	
	public PortForwardProxy(Reactor reactor, SocketChannel clientChannel, SocketChannel targetChannel) {
		this.reactor = reactor;
		this.clientChannel = clientChannel;
		this.targetChannel = targetChannel;
		queue = new LinkedBlockingDeque<>();
	}

	public void onReceiveMessage(Message m) {
//		System.out.println("Incoming message from: " + m.getChannel());
		queue.offer(m);
	}

	public synchronized void run() {
		Message m;
		while((m = queue.poll()) != null){
			int i = m.getChannel() == clientChannel?0:1;
			System.out.println("Message in readQueue=" + m + " i=" + i);
			
			SocketChannel[] v = new SocketChannel[]{clientChannel, targetChannel};
			
			reactor.sendMessage(new Message(v[1-i], m.getBuffer()));
		}
	}
}
