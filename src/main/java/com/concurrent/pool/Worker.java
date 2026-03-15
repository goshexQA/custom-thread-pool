package com.concurrent.pool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Worker implements Runnable {
    private static final Logger logger = Logger.getLogger(Worker.class.getName());
    
    private final BlockingQueue<Runnable> taskQueue;
    private final String name;
    private final CustomThreadPoolExecutor pool;
    private volatile boolean running = true;
    
    public Worker(BlockingQueue<Runnable> taskQueue, String name, CustomThreadPoolExecutor pool) {
        this.taskQueue = taskQueue;
        this.name = name;
        this.pool = pool;
    }
    
    @Override
    public void run() {
        logger.info("[Worker] " + name + " started");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Runnable task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                
                if (task != null) {
                    logger.info("[Worker] " + name + " executes task");
                    task.run();
                    pool.taskCompleted(); // Уведомляем о завершении задачи
                }
                
            } catch (InterruptedException e) {
                logger.info("[Worker] " + name + " interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        logger.info("[Worker] " + name + " terminated");
    }
    
    public void shutdown() {
        running = false;
        Thread.currentThread().interrupt();
    }
}