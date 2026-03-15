package com.concurrent.pool;

@FunctionalInterface
public interface RejectedTaskHandler {
    void rejected(Runnable task, CustomThreadPoolExecutor executor);
}