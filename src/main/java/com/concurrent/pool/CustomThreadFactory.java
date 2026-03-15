package com.concurrent.pool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class CustomThreadFactory implements ThreadFactory {
    private static final Logger logger = Logger.getLogger(CustomThreadFactory.class.getName());
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    
    public CustomThreadFactory(String poolName) {
        this.namePrefix = "MyPool-" + poolName + "-worker-";
    }
    
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
        thread.setDaemon(false);
        logger.info("[ThreadFactory] Creating new thread: " + thread.getName());
        return thread;
    }
}