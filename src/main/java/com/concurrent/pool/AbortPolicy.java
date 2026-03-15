package com.concurrent.pool;

import java.util.logging.Logger;

public class AbortPolicy implements RejectedTaskHandler {
    private static final Logger logger = Logger.getLogger(AbortPolicy.class.getName());
    
    @Override
    public void rejected(Runnable task, CustomThreadPoolExecutor executor) {
        logger.warning("[Rejected] Task rejected");
        throw new RuntimeException("Task rejected");
    }
}