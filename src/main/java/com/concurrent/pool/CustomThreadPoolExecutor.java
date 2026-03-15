package com.concurrent.pool;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class CustomThreadPoolExecutor implements CustomExecutor {
    private static final Logger logger = Logger.getLogger(CustomThreadPoolExecutor.class.getName());

    // Параметры пула
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;

    // Очереди задач (round robin распределение)
    private final List<BlockingQueue<Runnable>> taskQueues;
    private final AtomicInteger queueIndex = new AtomicInteger(0);

    // Рабочие потоки
    private final List<Worker> workers = new CopyOnWriteArrayList<>();
    private final Set<Worker> activeWorkers = ConcurrentHashMap.newKeySet();

    // Управление
    private final CustomThreadFactory threadFactory;
    private final RejectedTaskHandler rejectedHandler;
    private final ReentrantLock stateLock = new ReentrantLock();

    private volatile boolean isShutdown = false;
    private volatile boolean isTerminated = false;

    public CustomThreadPoolExecutor(Builder builder) {
        this.corePoolSize = builder.corePoolSize;
        this.maxPoolSize = builder.maxPoolSize;
        this.keepAliveTime = builder.keepAliveTime;
        this.timeUnit = builder.timeUnit;
        this.queueSize = builder.queueSize;
        this.minSpareThreads = builder.minSpareThreads;
        this.threadFactory = new CustomThreadFactory(builder.poolName);
        this.rejectedHandler = builder.rejectedHandler;

        // Создаем очереди (по одной на поток)
        this.taskQueues = new ArrayList<>(maxPoolSize);
        for (int i = 0; i < maxPoolSize; i++) {
            taskQueues.add(new LinkedBlockingQueue<>(queueSize));
        }

        // Запускаем core потоки
        for (int i = 0; i < corePoolSize; i++) {
            startNewWorker();
        }

        logger.info("[Pool] Initialized with core=" + corePoolSize +
                ", max=" + maxPoolSize + ", queueSize=" + queueSize);
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) throw new NullPointerException();

        if (isShutdown) {
            logger.warning("[Pool] Attempted to execute task on shutdown pool");
            return;
        }

        String taskDesc = command.toString();

        // Проверяем необходимость создания новых потоков
        ensureMinSpareThreads();

        // Выбираем очередь по алгоритму балансировки (Least Loaded)
        int queueId = selectQueue();
        BlockingQueue<Runnable> selectedQueue = taskQueues.get(queueId);

        // Пытаемся добавить задачу в очередь
        boolean offered = selectedQueue.offer(command);

        if (offered) {
            logger.info("[Pool] Task accepted into queue #" + queueId + ": " + taskDesc);

            // Если есть свободные потоки, они подхватят задачу автоматически
            // Если нет, создаем новый поток при необходимости
            if (workers.size() < maxPoolSize && !hasIdleWorkers()) {
                startNewWorker();
            }
        } else {
            // Очередь переполнена - применяем политику отказа
            rejectedHandler.rejected(command, this);
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> futureTask = new FutureTask<>(callable);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public void shutdown() {
        stateLock.lock();
        try {
            isShutdown = true;
            logger.info("[Pool] Shutdown initiated");

            // Прерываем idle потоки
            for (Worker worker : workers) {
                if (worker.isIdle()) {
                    worker.shutdown();
                }
            }
        } finally {
            stateLock.unlock();
        }
    }

    @Override
    public void shutdownNow() {
        stateLock.lock();
        try {
            isShutdown = true;
            logger.info("[Pool] ShutdownNow initiated");

            // Прерываем все потоки
            for (Worker worker : workers) {
                worker.shutdown();
            }

            // Очищаем очереди
            for (BlockingQueue<Runnable> queue : taskQueues) {
                queue.clear();
            }
        } finally {
            stateLock.unlock();
        }
    }

    // Вспомогательные методы

    private void startNewWorker() {
        stateLock.lock();
        try {
            if (workers.size() >= maxPoolSize || isShutdown) {
                return;
            }

            int workerId = workers.size();
            BlockingQueue<Runnable> queue = taskQueues.get(workerId);
            String workerName = threadFactory.newThread(() -> {}).getName();

            Worker worker = new Worker(this, queue, workerName);
            workers.add(worker);
            activeWorkers.add(worker);

            Thread thread = threadFactory.newThread(worker);
            thread.start();
        } finally {
            stateLock.unlock();
        }
    }

    private void ensureMinSpareThreads() {
        long idleCount = countIdleWorkers();
        if (idleCount < minSpareThreads && workers.size() < maxPoolSize) {
            int threadsToAdd = Math.min(minSpareThreads - (int) idleCount,
                    maxPoolSize - workers.size());
            for (int i = 0; i < threadsToAdd; i++) {
                startNewWorker();
            }
        }
    }

    private int selectQueue() {
        // Least Loaded алгоритм - выбираем очередь с наименьшим количеством задач
        int selected = 0;
        int minSize = Integer.MAX_VALUE;

        for (int i = 0; i < taskQueues.size(); i++) {
            int size = taskQueues.get(i).size();
            if (size < minSize) {
                minSize = size;
                selected = i;
                if (minSize == 0) break;
            }
        }

        return selected;
    }

    private long countIdleWorkers() {
        return workers.stream().filter(Worker::isIdle).count();
    }

    private boolean hasIdleWorkers() {
        return countIdleWorkers() > 0;
    }

    public boolean canWorkerTerminate() {
        return workers.size() > corePoolSize || isShutdown;
    }

    public void workerTerminated(Worker worker) {
        activeWorkers.remove(worker);
        workers.remove(worker);

        if (isShutdown && activeWorkers.isEmpty()) {
            isTerminated = true;
            logger.info("[Pool] All workers terminated, pool is fully shutdown");
        }
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    // Builder pattern для удобной конфигурации
    public static class Builder {
        private int corePoolSize = 2;
        private int maxPoolSize = 4;
        private long keepAliveTime = 5;
        private TimeUnit timeUnit = TimeUnit.SECONDS;
        private int queueSize = 10;
        private int minSpareThreads = 1;
        private String poolName = "default";
        private RejectedTaskHandler rejectedHandler = new AbortPolicy();

        public Builder corePoolSize(int size) { this.corePoolSize = size; return this; }
        public Builder maxPoolSize(int size) { this.maxPoolSize = size; return this; }
        public Builder keepAliveTime(long time) { this.keepAliveTime = time; return this; }
        public Builder timeUnit(TimeUnit unit) { this.timeUnit = unit; return this; }
        public Builder queueSize(int size) { this.queueSize = size; return this; }
        public Builder minSpareThreads(int count) { this.minSpareThreads = count; return this; }
        public Builder poolName(String name) { this.poolName = name; return this; }
        public Builder rejectedHandler(RejectedTaskHandler handler) {
            this.rejectedHandler = handler;
            return this;
        }

        public CustomThreadPoolExecutor build() {
            validate();
            return new CustomThreadPoolExecutor(this);
        }

        private void validate() {
            if (corePoolSize <= 0) throw new IllegalArgumentException("corePoolSize must be positive");
            if (maxPoolSize < corePoolSize) throw new IllegalArgumentException("maxPoolSize must be >= corePoolSize");
            if (queueSize <= 0) throw new IllegalArgumentException("queueSize must be positive");
            if (minSpareThreads < 0) throw new IllegalArgumentException("minSpareThreads must be non-negative");
        }
    }
}