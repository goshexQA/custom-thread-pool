package com.concurrent.pool;

import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CustomThreadPoolExecutorTest {
    
    private CustomThreadPoolExecutor pool;
    
    @BeforeEach
    void setUp() {
        pool = new CustomThreadPoolExecutor.Builder()
            .corePoolSize(2)
            .maxPoolSize(4)
            .queueSize(5)
            .keepAliveTime(1)
            .timeUnit(TimeUnit.SECONDS)
            .poolName("test")
            .build();
    }
    
    @AfterEach
    void tearDown() throws InterruptedException {
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }
    
    @Test
    void testExecute() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);
        
        for (int i = 0; i < 3; i++) {
            pool.execute(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(3, counter.get());
    }
    
    @Test
    void testSubmit() throws ExecutionException, InterruptedException {
        Future<String> future = pool.submit(() -> {
            Thread.sleep(100);
            return "Success";
        });
        
        assertEquals("Success", future.get(5, TimeUnit.SECONDS));
    }
    
    @Test
    void testRejectionPolicy() {
        CustomThreadPoolExecutor smallPool = new CustomThreadPoolExecutor.Builder()
            .corePoolSize(1)
            .maxPoolSize(1)
            .queueSize(1)
            .rejectedHandler(new AbortPolicy())
            .build();
        
        // Fill the pool
        smallPool.execute(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
        });
        
        smallPool.execute(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
        });
        
        // This should be rejected
        assertThrows(RuntimeException.class, () -> {
            smallPool.execute(() -> {});
        });
        
        smallPool.shutdown();
    }
    
    @Test
    void testShutdown() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        
        for (int i = 0; i < 5; i++) {
            pool.execute(counter::incrementAndGet);
        }
        
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(5, counter.get());
    }
    
    @Test
    void testShutdownNow() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(1);
        
        pool.execute(() -> {
            startLatch.countDown();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                endLatch.countDown();
            }
        });
        
        assertTrue(startLatch.await(1, TimeUnit.SECONDS));
        pool.shutdownNow();
        assertTrue(endLatch.await(1, TimeUnit.SECONDS));
    }
}
