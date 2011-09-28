package com.deltapunkt.secproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import com.deltapunkt.secproxy.interfaces.AcceptHandler;
import com.deltapunkt.secproxy.interfaces.ConnectHandler;
import com.deltapunkt.secproxy.interfaces.MessageConsumer;
import com.deltapunkt.secproxy.interfaces.Reactor;

public class DefaultReactor implements Reactor, Runnable {
	private static final int OP_NONE = 0;

	private volatile boolean running;
	private final ExecutorService es;
	private final Selector selector;
	private final BlockingQueue<Runnable> commandQueue;

	private final ConnectionManager connectionManager;

	public DefaultReactor(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
		es = Executors.newSingleThreadExecutor();
		try {
			selector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Selector.open() failed!", e);
		}
		commandQueue = new LinkedBlockingDeque<Runnable>();
	}

	public void start() {
		es.execute(this);
	}

	public void run() {
		running = true;
		while (running) {
			try {
				while (selector.keys().isEmpty()) {
					// wait here for ServerSocket registration
					commandQueue.take().run();
				}
				processCommandQueue();
				try {
					selector.select();

					for (SelectionKey key : selector.selectedKeys()) {
						((Runnable) key.attachment()).run();
					}
					selector.selectedKeys().clear();

				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (InterruptedException ie) {
				ie.printStackTrace();
				Thread.currentThread().interrupt();
				running = false;
			}
		}
	}

	private void processCommandQueue() {
		Runnable r;
		while ((r = commandQueue.poll()) != null) {
			r.run();
		}
	}

	class AcceptCommand implements Runnable {
		private ServerSocketChannel serverSocketChannel;
		private AcceptHandler acceptHandler;

		public AcceptCommand(ServerSocketChannel serverSocketChannel,
				AcceptHandler acceptHandler) {
			this.serverSocketChannel = serverSocketChannel;
			this.acceptHandler = acceptHandler;
		}

		public void run() {
			try {
				SocketChannel socketChannel = serverSocketChannel.accept();
				socketChannel.configureBlocking(false);
				socketChannel.register(selector, OP_NONE);
				acceptHandler.onAccept(DefaultReactor.this, socketChannel);
			} catch (Exception e) {
				// more complex exception handling
				e.printStackTrace();
				throw new RuntimeException("Accept Error!", e);
			}
		}
	}

	public void registerAcceptor(SocketAddress acceptAddress,
			AcceptHandler acceptHandler) {
		commandQueue.offer(new RegisterAcceptorCommand(acceptAddress,
				acceptHandler));
		selector.wakeup();
	}

	class RegisterAcceptorCommand implements Runnable {
		private SocketAddress acceptAddress;
		private AcceptHandler acceptHandler;

		public RegisterAcceptorCommand(SocketAddress acceptAddress,
				AcceptHandler acceptHandler) {
			this.acceptAddress = acceptAddress;
			this.acceptHandler = acceptHandler;
		}

		public void run() {
			try {
				ServerSocketChannel serverSocketChannel = createServerSocket(acceptAddress);
				serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT,
						new AcceptCommand(serverSocketChannel, acceptHandler));
				System.out.println("Acceptor added!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private ServerSocketChannel createServerSocket(SocketAddress acceptAddress)
				throws IOException, SocketException {

			ServerSocketChannel serverSocketChannel = ServerSocketChannel
					.open();
			serverSocketChannel.configureBlocking(false);
			ServerSocket serverSocket = serverSocketChannel.socket();
			serverSocket.setReuseAddress(true);
			serverSocket.bind(acceptAddress);

			return serverSocketChannel;
		}
	}

	public void connect(final SocketAddress targetAddress,
			final ConnectHandler connectHandler) {
		commandQueue.offer(new Runnable() {
			private SocketChannel sc;

			public void run() {
				try {
					sc = SocketChannel.open();
					sc.configureBlocking(false);
					sc.connect(targetAddress);
					Runnable connectCommand = new Runnable() {
						public void run() {
							try {
								sc.finishConnect();
								sc.register(selector, OP_NONE);
								connectHandler.onConnect(DefaultReactor.this,
										sc);
							} catch (IOException e) {
								// more complex exception handling
								e.printStackTrace();
								throw new RuntimeException("Connect Error!", e);
							}
						}
					};
					sc.register(selector, SelectionKey.OP_CONNECT,
							connectCommand);
					System.out.println("Listener added!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		selector.wakeup();
	}

	public void makeChannelReadable(final SocketChannel sc) {
		commandQueue.offer(new Runnable() {
			public void run() {
				try {
					Runnable readCommand = new Runnable() {
						public void run() {
							try {
								connectionManager.onRead(sc);
							} catch (Exception e) {
								// more complex exception handling
								e.printStackTrace();
								throw new RuntimeException("Read Error!", e);
							}
						}
					};
					sc.register(selector, SelectionKey.OP_READ, readCommand);
				} catch (Exception e) {
					System.out.println("sc=" + sc);
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		});
		selector.wakeup();
	}

	public void makeChannelWritable(final SocketChannel sc) {
		commandQueue.offer(new Runnable() {
			public void run() {
				Runnable writeCommand = new Runnable() {
					public void run() {
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
					}
				};
				try {
					sc.register(selector, SelectionKey.OP_WRITE, writeCommand);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("sc=" + sc);
					throw new RuntimeException(e);
				}
			}
		});
		selector.wakeup();
	}

	public void registerChannelHandler(SocketChannel sc,
			MessageConsumer messageConsumer) {
		connectionManager.addConnection(sc, messageConsumer);
	}

	public void sendMessage(Message message) {
		// add to cmd queue
		connectionManager.sendMessage(message);
		makeChannelWritable(message.getChannel());
	}

	public void stop() {
		running = false;
		selector.wakeup();
		es.shutdown();
	}
}
