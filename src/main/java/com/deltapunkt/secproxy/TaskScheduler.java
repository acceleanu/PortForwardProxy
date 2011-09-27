package com.deltapunkt.secproxy;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class TaskScheduler implements Runnable {
	private BlockingDeque<Runnable> taskQueue;
	private ExecutorService exec;
	private volatile Thread	schedulerThread;

	public TaskScheduler(int n) {
		taskQueue = new LinkedBlockingDeque<Runnable>(n);
		exec = Executors.newFixedThreadPool(n + 1);
		exec.execute(this);
	}

	public void run() {
		schedulerThread = Thread.currentThread();
		while (schedulerThread == Thread.currentThread()) {
			try {
				Runnable r = taskQueue.take();
				exec.execute(r);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		exec.shutdown();
	}

	public void addTask(Runnable r) {
		try {
			taskQueue.put(r);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		schedulerThread.interrupt();
		schedulerThread = null;
	}
}
