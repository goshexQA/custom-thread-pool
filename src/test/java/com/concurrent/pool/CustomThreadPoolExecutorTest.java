package com.concurrent.pool;

import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CustomThreadPoolExecutorTest {
    
    @Test
    void testExecute() throws Exception {
        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
            .corePoolSize(2)
            .queueSize(5)
            .build();
        
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);
        
        for (int i = 0; i < 3; i++) {
            pool.execute(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Tasks did not complete");
        assertEquals(3, counter.get());
        
        pool.shutdown();
    }
    
    @Test
    void testSubmit() throws Exception {
        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
            .corePoolSize(1)
            .build();
        
        Future<String> future = pool.submit(() -> "Success");
        
        String result = future.get(2, TimeUnit.SECONDS);
        assertEquals("Success", result);
        
        pool.shutdown();
    }
    
    @Test
    void testMultipleTasks() throws Exception {
        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
            .corePoolSize(4)
            .queueSize(20)
            .build();
        
        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger counter = new AtomicInteger(0);
        
        for (int i = 0; i < taskCount; i++) {
            pool.execute(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Tasks did not complete");
        assertEquals(taskCount, counter.get());
        
        pool.shutdown();
    }
}