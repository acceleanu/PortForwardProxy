package com.deltapunkt.secproxy;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
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
	private final BlockingQueue<Runnable> cmdQueue;
//	private final ConcurrentHashMap<ServerSocketChannel, AcceptHandler> acceptorMap;

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
		cmdQueue = new LinkedBlockingDeque<Runnable>();
//		acceptorMap = new ConcurrentHashMap<ServerSocketChannel, AcceptHandler>();
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
					cmdQueue.take().run();
				}
				processCmdQueue();
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

	private void processCmdQueue() {
		Runnable r;
		while ((r = cmdQueue.poll()) != null) {
			r.run();
		}
	}

	private Reactor getThisReactor() {
		return this;
	}

	public void addPortListener(final SocketAddress sa,
			final AcceptHandler acceptHandler) {
		final CountDownLatch sync = new CountDownLatch(1);
		cmdQueue.offer(new Runnable() {
			ServerSocketChannel ssc;

			public void run() {
				try {
					ssc = ServerSocketChannel.open();
					ssc.configureBlocking(false);
					ssc.socket().setReuseAddress(true);
					ssc.socket().bind(sa);
					Runnable acceptCommand = new Runnable() {
						public void run() {
							try {
								SocketChannel sc = ssc.accept();
								sc.configureBlocking(false);
								sc.register(selector, OP_NONE);
								// acceptorMap.get(ssc).onAccept(getThis(), sc);
								acceptHandler.onAccept(getThisReactor(), sc);
							} catch (Exception e) {
								// more complex exception handling
								e.printStackTrace();
								throw new RuntimeException("Accept Error!", e);
							}
						}
					};
					ssc.register(selector, SelectionKey.OP_ACCEPT,
							acceptCommand);
					// acceptorMap.put(ssc, acceptHandler);
					sync.countDown();
					System.out.println("Listener added!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		selector.wakeup();
		try {
			sync.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void connect(final SocketAddress targetAddress,
			final ConnectHandler connectHandler) {
		cmdQueue.offer(new Runnable() {
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
								connectHandler.onConnect(getThisReactor(), sc);
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
		cmdQueue.offer(new Runnable() {
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
		cmdQueue.offer(new Runnable() {
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
