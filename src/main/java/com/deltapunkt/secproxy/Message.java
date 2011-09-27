package com.deltapunkt.secproxy;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Message {
	private final SocketChannel sc;
	private final ByteBuffer buffer;

	public Message(SocketChannel sc, String str) {
		this.sc = sc;
		this.buffer = ByteBuffer.wrap(str.getBytes());
	}

	public Message(SocketChannel sc, ByteBuffer buffer) {
		this.sc = sc;
		this.buffer = buffer;
	}

	public SocketChannel getChannel() {
		return sc;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public String getAsString() {
		byte v[] = new byte[buffer.limit()];
		buffer.get(v);
		
		return new String(v);
	}
}
