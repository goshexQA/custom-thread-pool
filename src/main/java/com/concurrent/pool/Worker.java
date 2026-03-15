package com.concurrent.pool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Worker implements Runnable {
    private static final Logger logger = Logger.getLogger(Worker.class.getName());

    private final CustomThreadPoolExecutor pool;
    private final BlockingQueue<Runnable> taskQueue;
    private final String name;
    private volatile boolean running = true;
    private volatile boolean idle = false;
    private long lastTaskTime = System.currentTimeMillis();

    public Worker(CustomThreadPoolExecutor pool, BlockingQueue<Runnable> taskQueue, String name) {
        this.pool = pool;
        this.taskQueue = taskQueue;
        this.name = name;
    }

    @Override
    public void run() {
        logger.info("[Worker] " + name + " started");

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Проверяем состояние пула
                if (pool.isShutdown() && taskQueue.isEmpty()) {
                    break;
                }

                // Пытаемся получить задачу с таймаутом
                Runnable task = taskQueue.poll(pool.getKeepAliveTime(), pool.getTimeUnit());

                if (task != null) {
                    // Получили задачу - выполняем
                    idle = false;
                    lastTaskTime = System.currentTimeMillis();

                    logger.info("[Worker] " + name + " executes " + task.toString());

                    try {
                        task.run();
                    } catch (Exception e) {
                        logger.severe("[Worker] " + name + " failed to execute task: " + e);
                    }
                } else {
                    // Таймаут - нет задач
                    handleIdleTimeout();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("[Worker] " + name + " terminated");
        pool.workerTerminated(this);
    }

    private void handleIdleTimeout() {
        if (!idle) {
            idle = true;
            logger.info("[Worker] " + name + " idle timeout, checking termination condition");
        }

        // Проверяем, можем ли мы завершиться
        if (pool.canWorkerTerminate()) {
            running = false;
        }
    }

    public void shutdown() {
        running = false;
        Thread.currentThread().interrupt();
    }

    public boolean isIdle() {
        return idle;
    }

    public String getName() {
        return name;
    }
}