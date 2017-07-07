package com.deltapunkt.secproxy;

import com.deltapunkt.secproxy.interfaces.LifeCycle;
import com.deltapunkt.secproxy.interfaces.Reactor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Defines the execution policy for the Reactor.
 * 
 */
public class ReactorProcess implements LifeCycle, Runnable {
	private final Reactor reactor;
	private final ExecutorService executorService;
	private volatile boolean running;

	public static LifeCycle create(Reactor reactor) {
		ExecutorService executorService = Executors.newSingleThreadExecutor();

		return new ReactorProcess(reactor, executorService);
	}

	private ReactorProcess(Reactor reactor, ExecutorService executorService) {
		this.reactor = reactor;
		this.executorService = executorService;
	}

	public void start() {
		executorService.execute(this);
	}

	public void stop() {
		running = false;
		reactor.wakeup();
		executorService.shutdown();
	}

	public void run() {
		running = true;
		while (running) {
			try {
				reactor.processQueues();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
				Thread.currentThread().interrupt();
				running = false;
			}
		}
	}
}
