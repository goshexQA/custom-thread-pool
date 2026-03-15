package com.concurrent.pool;

import java.util.logging.Logger;

@FunctionalInterface
public interface RejectedTaskHandler {
    void rejected(Runnable task, CustomThreadPoolExecutor executor);
}

class AbortPolicy implements RejectedTaskHandler {
    private static final Logger logger = Logger.getLogger(AbortPolicy.class.getName());

    @Override
    public void rejected(Runnable task, CustomThreadPoolExecutor executor) {
        String taskDesc = task.toString();
        logger.warning("[Rejected] Task " + taskDesc + " was rejected due to overload!");
        throw new RuntimeException("Task rejected: " + taskDesc);
    }
}

class CallerRunsPolicy implements RejectedTaskHandler {
    private static final Logger logger = Logger.getLogger(CallerRunsPolicy.class.getName());

    @Override
    public void rejected(Runnable task, CustomThreadPoolExecutor executor) {
        String taskDesc = task.toString();
        logger.info("[Rejected] Executing task " + taskDesc + " in caller thread");
        task.run();
    }
}

class DiscardPolicy implements RejectedTaskHandler {
    private static final Logger logger = Logger.getLogger(DiscardPolicy.class.getName());

    @Override
    public void rejected(Runnable task, CustomThreadPoolExecutor executor) {
        String taskDesc = task.toString();
        logger.warning("[Rejected] Task " + taskDesc + " was discarded silently");
    }
}