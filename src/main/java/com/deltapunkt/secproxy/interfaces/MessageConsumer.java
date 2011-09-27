package com.deltapunkt.secproxy.interfaces;

import com.deltapunkt.secproxy.Message;

/*
 * consumes a message
 */
public interface MessageConsumer extends Runnable {
	void onReceiveMessage(Message m);
}
