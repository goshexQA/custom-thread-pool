package com.concurrent.pool;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class CustomThreadPoolExecutor implements CustomExecutor {
    private static final Logger logger = Logger.getLogger(CustomThreadPoolExecutor.class.getName());
    
    private final int corePoolSize;
    private final BlockingQueue<Runnable> workQueue;
    private final List<Worker> workers = new ArrayList<>();
    private final CustomThreadFactory threadFactory;
    private final RejectedTaskHandler rejectedHandler;
    
    private volatile boolean isShutdown = false;
    private volatile boolean isTerminated = false;
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    
    public CustomThreadPoolExecutor(Builder builder) {
        this.corePoolSize = builder.corePoolSize;
        this.workQueue = new LinkedBlockingQueue<>(builder.queueSize);
        this.threadFactory = new CustomThreadFactory(builder.poolName);
        this.rejectedHandler = builder.rejectedHandler;
        
        // Запускаем core потоки
        for (int i = 0; i < corePoolSize; i++) {
            startWorker();
        }
        
        logger.info("[Pool] Initialized with core=" + corePoolSize + ", queueSize=" + builder.queueSize);
    }
    
    private void startWorker() {
        String workerName = threadFactory.newThread(() -> {}).getName();
        Worker worker = new Worker(workQueue, workerName, this);
        workers.add(worker);
        Thread thread = threadFactory.newThread(worker);
        thread.start();
    }
    
    @Override
    public void execute(Runnable command) {
        if (command == null) throw new NullPointerException();
        
        if (isShutdown) {
            logger.warning("[Pool] Pool is shutdown");
            return;
        }
        
        activeTasks.incrementAndGet();
        boolean offered = workQueue.offer(command);
        
        if (!offered) {
            activeTasks.decrementAndGet();
            rejectedHandler.rejected(command, this);
        } else {
            logger.info("[Pool] Task accepted");
        }
    }
    
    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> futureTask = new FutureTask<>(callable) {
            @Override
            protected void done() {
                activeTasks.decrementAndGet();
            }
        };
        execute(futureTask);
        return futureTask;
    }
    
    // Метод для уведомления о завершении задачи
    public void taskCompleted() {
        activeTasks.decrementAndGet();
    }
    
    @Override
    public void shutdown() {
        isShutdown = true;
        logger.info("[Pool] Shutdown initiated");
        
        for (Worker worker : workers) {
            worker.shutdown();
        }
    }
    
    @Override
    public void shutdownNow() {
        isShutdown = true;
        logger.info("[Pool] ShutdownNow initiated");
        
        for (Worker worker : workers) {
            worker.shutdown();
        }
        workQueue.clear();
    }
    
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        
        while (activeTasks.get() > 0) {
            long now = System.currentTimeMillis();
            if (now >= deadline) {
                return false;
            }
            Thread.sleep(10);
        }
        
        // Даем время потокам завершиться
        Thread.sleep(100);
        isTerminated = true;
        return true;
    }
    
    public static class Builder {
        private int corePoolSize = 2;
        private int queueSize = 10;
        private String poolName = "default";
        private RejectedTaskHandler rejectedHandler = new AbortPolicy();
        
        public Builder corePoolSize(int size) { 
            this.corePoolSize = size; 
            return this; 
        }
        
        public Builder queueSize(int size) { 
            if (size <= 0) throw new IllegalArgumentException("queueSize must be positive");
            this.queueSize = size; 
            return this; 
        }
        
        public Builder poolName(String name) { 
            this.poolName = name; 
            return this; 
        }
        
        public Builder rejectedHandler(RejectedTaskHandler handler) { 
            this.rejectedHandler = handler; 
            return this; 
        }
        
        public CustomThreadPoolExecutor build() {
            if (corePoolSize <= 0) throw new IllegalArgumentException("corePoolSize must be positive");
            return new CustomThreadPoolExecutor(this);
        }
    }
}