package com.concurrent.pool;

import java.util.logging.Logger;

public class CallerRunsPolicy implements RejectedTaskHandler {
    private static final Logger logger = Logger.getLogger(CallerRunsPolicy.class.getName());
    
    @Override
    public void rejected(Runnable task, CustomThreadPoolExecutor executor) {
        String taskDesc = task.toString();
        logger.info("[Rejected] Executing task " + taskDesc + " in caller thread");
        task.run();
    }
}