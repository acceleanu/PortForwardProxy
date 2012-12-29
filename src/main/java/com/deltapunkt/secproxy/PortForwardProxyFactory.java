package com.deltapunkt.secproxy;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import com.deltapunkt.secproxy.interfaces.ConnectHandler;
import com.deltapunkt.secproxy.interfaces.Proxy;
import com.deltapunkt.secproxy.interfaces.ProxyFactory;
import com.deltapunkt.secproxy.interfaces.Reactor;

public class PortForwardProxyFactory implements ProxyFactory {
	private final SocketAddress targetAddress;
	private final Reactor reactor;

	public PortForwardProxyFactory(Reactor reactor, SocketAddress targetAddress) {
		this.reactor = reactor;
		this.targetAddress = targetAddress;
	}

	public void onAccept(final SocketChannel clientChannel) {
		System.out.println("Client connection accepted! " + clientChannel);
		reactor.registerConnector(targetAddress, new ConnectHandler() {
			public void onConnect(SocketChannel targetChannel) {
				System.out.println("Target connection completed! ");
				Proxy proxy = new PortForwardProxy(reactor, clientChannel,
						targetChannel);
				reactor.registerChannelHandler(clientChannel, proxy);
				reactor.registerChannelHandler(targetChannel, proxy);
				reactor.makeChannelReadable(clientChannel);
				reactor.makeChannelReadable(targetChannel);
			}
		});
	}
}
