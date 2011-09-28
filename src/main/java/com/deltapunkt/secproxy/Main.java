package com.deltapunkt.secproxy;


import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.deltapunkt.secproxy.interfaces.ProxyFactory;
import com.deltapunkt.secproxy.interfaces.Reactor;

public class Main 
{
    private static final int SERVER_PORT = 22;
	private static final int PROXY_PORT = 2222;

	public static void main( String[] args )
    {
		TaskScheduler taskScheduler = new TaskScheduler(10);
		ConnectionManager connectionHandler = new PeerConnectionManager(taskScheduler);
		
		Reactor reactor = new DefaultReactor(connectionHandler);
		reactor.start();
		SocketAddress targetAddress = new InetSocketAddress("localhost", SERVER_PORT);
		ProxyFactory pf = new PortForwardProxyFactory(targetAddress);
		SocketAddress proxyAddress = new InetSocketAddress("localhost", PROXY_PORT);
		reactor.registerAcceptor(proxyAddress, pf);
    }
}
