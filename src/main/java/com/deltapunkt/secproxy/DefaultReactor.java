package com.deltapunkt.secproxy;

import com.deltapunkt.secproxy.interfaces.AcceptHandler;
import com.deltapunkt.secproxy.interfaces.ConnectHandler;
import com.deltapunkt.secproxy.interfaces.MessageConsumer;
import com.deltapunkt.secproxy.interfaces.Reactor;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class DefaultReactor implements Reactor {
	private static final int OP_NONE = 0;

	private final Selector selector;
	private final BlockingQueue<Runnable> commandQueue;
	private final ConnectionManager connectionManager;

	public static DefaultReactor create(ConnectionManager connectionManager) {
		Selector selector;
		try {
			selector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Selector.open() failed!", e);
		}

		BlockingQueue<Runnable> commandQueue = new LinkedBlockingDeque<>();
		return new DefaultReactor(connectionManager, selector, commandQueue);
	}

	private DefaultReactor(ConnectionManager connectionManager,
			Selector selector, BlockingQueue<Runnable> commandQueue) {
		this.connectionManager = connectionManager;
		this.selector = selector;
		this.commandQueue = commandQueue;
	}

	private void processCommandQueue() throws InterruptedException {
		while (selector.keys().isEmpty()) {
			// wait here for ServerSocket registration
			commandQueue.take().run();
		}
		Runnable r;
		while ((r = commandQueue.poll()) != null) {
			r.run();
		}
	}

	class NIOAcceptEventHandler implements Runnable {
		private final ServerSocketChannel serverChannel;
		private final AcceptHandler acceptHandler;

		public NIOAcceptEventHandler(ServerSocketChannel serverChannel,
				AcceptHandler acceptHandler) {
			this.serverChannel = serverChannel;
			this.acceptHandler = acceptHandler;
		}

		public void run() {
			try {
				SocketChannel clientChannel = serverChannel.accept();
				clientChannel.configureBlocking(false);
				clientChannel.register(selector, OP_NONE);
				acceptHandler.onAccept(clientChannel);
			} catch (Exception e) {
				// more complex exception handling
				e.printStackTrace();
				throw new RuntimeException("Accept Error!", e);
			}
		}
	}

	class NIORegisterAcceptorCommand implements Runnable {
		private final SocketAddress acceptAddress;
		private final AcceptHandler acceptHandler;

		public NIORegisterAcceptorCommand(SocketAddress acceptAddress,
				AcceptHandler acceptHandler) {
			this.acceptAddress = acceptAddress;
			this.acceptHandler = acceptHandler;
		}

		public void run() {
			try {
				ServerSocketChannel serverChannel = NIOUtil
						.createServerChannel(acceptAddress);
				serverChannel
						.register(selector, SelectionKey.OP_ACCEPT,
								new NIOAcceptEventHandler(serverChannel,
										acceptHandler));
				System.out.println("Acceptor registered!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void registerAcceptor(SocketAddress acceptAddress,
			AcceptHandler acceptHandler) {
		commandQueue.offer(new NIORegisterAcceptorCommand(acceptAddress,
				acceptHandler));
		selector.wakeup();
	}

	class NIOConnectEventHandler implements Runnable {
		private SocketChannel socketChannel;
		private ConnectHandler connectHandler;

		NIOConnectEventHandler(SocketChannel socketChannel,
				ConnectHandler connectHandler) {
			this.socketChannel = socketChannel;
			this.connectHandler = connectHandler;
		}

		public void run() {
			try {
				socketChannel.finishConnect();
				socketChannel.register(selector, OP_NONE);
				connectHandler.onConnect(socketChannel);
			} catch (IOException e) {
				// more complex exception handling
				e.printStackTrace();
				throw new RuntimeException("Connect Error!", e);
			}
		}
	}

	class NIORegisterConnectorCommand implements Runnable {
		private final SocketAddress targetAddress;
		private final ConnectHandler connectHandler;

		NIORegisterConnectorCommand(SocketAddress targetAddress,
				ConnectHandler connectHandler) {
			this.targetAddress = targetAddress;
			this.connectHandler = connectHandler;
		}

		public void run() {
			try {
				SocketChannel clientChannel = NIOUtil.createClientChannel();
				clientChannel.connect(targetAddress);

				clientChannel.register(selector, SelectionKey.OP_CONNECT,
						new NIOConnectEventHandler(clientChannel,
								connectHandler));
				System.out.println("Connector registered!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void registerConnector(SocketAddress connectAddress,
			ConnectHandler connectHandler) {
		commandQueue.offer(new NIORegisterConnectorCommand(connectAddress,
				connectHandler));
		selector.wakeup();
	}

	public void makeChannelReadable(final SocketChannel sc) {
		commandQueue.offer(() -> {
            try {
                Runnable readCommand = () -> {
                    try {
                        connectionManager.onRead(sc);
                    } catch (Exception e) {
                        // more complex exception handling
                        e.printStackTrace();
                        throw new RuntimeException("Read Error!", e);
                    }
                };
                sc.register(selector, SelectionKey.OP_READ, readCommand);
            } catch (Exception e) {
                System.out.println("sc=" + sc);
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
		selector.wakeup();
	}

	public void makeChannelWritable(final SocketChannel sc) {
		commandQueue.offer(() -> {
            Runnable writeCommand = () -> {
                try {
                    boolean finishedWriting = connectionManager
                            .onWrite(sc);
                    if (finishedWriting) {
                        makeChannelReadable(sc);
                    }
                } catch (Exception e) {
                    // more complex exception handling
                    e.printStackTrace();
                    throw new RuntimeException("Write Error!", e);
                }
            };
            try {
                sc.register(selector, SelectionKey.OP_WRITE, writeCommand);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("sc=" + sc);
                throw new RuntimeException(e);
            }
        });
		selector.wakeup();
	}

	public void registerChannelHandler(SocketChannel sc,
			MessageConsumer messageConsumer) {
		connectionManager.addConnection(sc, messageConsumer);
	}

	public void sendMessage(Message message) {
		connectionManager.sendMessage(message);
		makeChannelWritable(message.getChannel());
	}

	public void wakeup() {
		selector.wakeup();
	}

	public void processQueues() throws InterruptedException {
		processCommandQueue();
		try {
			selector.select();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Iterator<SelectionKey> it = selector.selectedKeys().iterator();
		while (it.hasNext()) {
			SelectionKey key = it.next();
			it.remove();
			try {
				((Runnable) key.attachment()).run();
			} catch (Exception e) {
				System.err
						.println("Caught while running the reactor event handler");
				e.printStackTrace();
			}
		}
	}
}
