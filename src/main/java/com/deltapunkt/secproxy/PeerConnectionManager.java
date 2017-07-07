package com.deltapunkt.secproxy;

import com.deltapunkt.secproxy.interfaces.MessageConsumer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class PeerConnectionManager implements ConnectionManager {

	private final Map<SocketChannel, BlockingDeque<ByteBuffer>> writeMap;
	private final Map<SocketChannel, MessageConsumer> consumerMap;
	private final TaskScheduler taskScheduler;

	public PeerConnectionManager(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
		consumerMap = new ConcurrentHashMap<>();
		writeMap = new ConcurrentHashMap<>();
	}

	public void addConnection(SocketChannel sc, MessageConsumer messageConsumer) {
		consumerMap.put(sc, messageConsumer);
		writeMap.put(sc, new LinkedBlockingDeque<>());
	}

	public void sendMessage(Message message) {
		BlockingDeque<ByteBuffer> queue = writeMap.get(message.getChannel());
		queue.offer(message.getBuffer());
	}

	/*
	 * 
	 * returns a flag indicating whether all the messages have been sent so that
	 * the caller can either set the channel to readable or leave it in writable
	 * mode until all messages have been sent
	 */
	public boolean onWrite(SocketChannel sc) {
		BlockingDeque<ByteBuffer> queue = writeMap.get(sc);

		boolean finished = true;
		while (!queue.isEmpty()) {
			ByteBuffer buffer = queue.getFirst();
			try {
				sc.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (buffer.remaining() > 0) {
				finished = false;
				break;
			}
			queue.removeFirst();
		}

		return finished;
	}

	public void onRead(SocketChannel sc) {
		ByteBuffer buffer = ByteBuffer.allocate(8192);
		try {
			sc.read(buffer);
			buffer.flip();
			Message m = new Message(sc, buffer);
			MessageConsumer messageConsumer = consumerMap.get(m.getChannel());
			messageConsumer.onReceiveMessage(m);
			taskScheduler.addTask(messageConsumer);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
