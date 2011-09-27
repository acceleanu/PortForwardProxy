package com.deltapunkt.secproxy;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import com.deltapunkt.secproxy.interfaces.ConnectHandler;
import com.deltapunkt.secproxy.interfaces.Proxy;
import com.deltapunkt.secproxy.interfaces.ProxyFactory;
import com.deltapunkt.secproxy.interfaces.Reactor;

public class PortForwardProxyFactory implements ProxyFactory {
	private final SocketAddress targetAddress;
	
	public PortForwardProxyFactory(SocketAddress targetAddress) {
		this.targetAddress = targetAddress;
	}

	public void onAccept(Reactor reactor, final SocketChannel clientChannel) {
		System.out.println("Client connection accepted! " + clientChannel);
		reactor.connect(targetAddress, new ConnectHandler() {
			public void onConnect(Reactor reactor, SocketChannel targetChannel) {
				System.out.println("Target connection completed! ");
				Proxy proxy = new PortForwardProxy(reactor, clientChannel, targetChannel);
				reactor.registerChannelHandler(clientChannel, proxy);
				reactor.registerChannelHandler(targetChannel, proxy);
				reactor.makeChannelReadable(clientChannel);
				reactor.makeChannelReadable(targetChannel);
			}
		});
	}
}
