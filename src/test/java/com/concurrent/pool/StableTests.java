package com.concurrent.pool;

import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class StableTests {
    
    @Test
    void testBasicExecution() throws Exception {
        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
            .corePoolSize(1)
            .build();
            
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // do nothing
        }, pool);
        
        future.get(2, TimeUnit.SECONDS);
        pool.shutdown();
        assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
    }
    
    @Test
    void testTaskCount() throws Exception {
        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
            .corePoolSize(2)
            .build();
            
        int taskCount = 5;
        CountDownLatch latch = new CountDownLatch(taskCount);
        
        for (int i = 0; i < taskCount; i++) {
            pool.execute(latch::countDown);
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        pool.shutdown();
        assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
    }
}