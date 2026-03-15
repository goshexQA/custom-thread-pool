package com.concurrent.pool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    static {
        // Настройка логирования для читаемого вывода
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%5$s%n");
    }

    public static void main(String[] args) throws InterruptedException {
        logger.info("=== Демонстрация работы кастомного пула потоков ===\n");

        // Тест 1: Базовая работа пула
        testBasicFunctionality();

        // Тест 2: Перегрузка и политика отказа
        testOverloadHandling();

        // Тест 3: Поддержание минимального числа резервных потоков
        testMinSpareThreads();

        // Тест 4: Shutdown и завершение
        testShutdown();
    }

    private static void testBasicFunctionality() throws InterruptedException {
        logger.info("\n--- Тест 1: Базовая работа пула ---");

        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .queueSize(3)
                .keepAliveTime(2)
                .timeUnit(TimeUnit.SECONDS)
                .minSpareThreads(1)
                .poolName("test1")
                .rejectedHandler(new CallerRunsPolicy())
                .build();

        // Отправляем 5 задач
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            pool.execute(() -> {
                try {
                    logger.info("=== Задача " + taskId + " начала выполнение");
                    Thread.sleep(1000); // Имитация работы
                    logger.info("=== Задача " + taskId + " завершена");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(10000); // Ждем выполнения
        pool.shutdown();
        Thread.sleep(2000);
    }

    private static void testOverloadHandling() throws InterruptedException {
        logger.info("\n--- Тест 2: Обработка перегрузки (AbortPolicy) ---");

        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
                .corePoolSize(1)
                .maxPoolSize(2)
                .queueSize(2)
                .keepAliveTime(1)
                .timeUnit(TimeUnit.SECONDS)
                .poolName("test2")
                .rejectedHandler(new AbortPolicy())
                .build();

        // Отправляем больше задач, чем может обработать пул
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            try {
                pool.execute(() -> {
                    try {
                        logger.info("=== Задача " + taskId + " выполняется");
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                Thread.sleep(100);
            } catch (Exception e) {
                logger.warning("!!! Задача " + taskId + " отклонена: " + e.getMessage());
            }
        }

        Thread.sleep(5000);
        pool.shutdown();
    }

    private static void testMinSpareThreads() throws InterruptedException {
        logger.info("\n--- Тест 3: Поддержание минимального числа резервных потоков ---");

        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
                .corePoolSize(2)
                .maxPoolSize(5)
                .queueSize(5)
                .keepAliveTime(2)
                .timeUnit(TimeUnit.SECONDS)
                .minSpareThreads(3)  // Минимум 3 свободных потока
                .poolName("test3")
                .rejectedHandler(new DiscardPolicy())
                .build();

        // Отправляем задачи с паузами, чтобы создать простой
        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            pool.execute(() -> {
                logger.info("=== Задача " + taskId + " (короткая) выполняется");
                try { Thread.sleep(500); } catch (InterruptedException e) {}
            });
            Thread.sleep(100);
        }

        Thread.sleep(3000); // Ждем, чтобы потоки вошли в idle

        // Должны увидеть, что minSpareThreads поддерживается
        logger.info("Отправляем новую задачу для проверки наличия резервных потоков");
        pool.execute(() -> {
            logger.info("=== Резервный поток мгновенно подхватил задачу");
        });

        Thread.sleep(3000);
        pool.shutdown();
    }

    private static void testShutdown() throws InterruptedException {
        logger.info("\n--- Тест 4: Проверка shutdown и graceful завершения ---");

        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
                .corePoolSize(3)
                .maxPoolSize(6)
                .queueSize(10)
                .poolName("test4")
                .build();

        // Отправляем долгие задачи
        for (int i = 1; i <= 8; i++) {
            final int taskId = i;
            pool.execute(() -> {
                try {
                    logger.info("=== Долгая задача " + taskId + " старт");
                    Thread.sleep(3000);
                    logger.info("=== Долгая задача " + taskId + " финиш");
                } catch (InterruptedException e) {
                    logger.info("=== Долгая задача " + taskId + " прервана");
                }
            });
        }

        Thread.sleep(1000);
        logger.info("Вызываем shutdown...");
        pool.shutdown();

        // Пытаемся добавить новую задачу после shutdown
        try {
            pool.execute(() -> logger.info("Эта задача не должна выполниться"));
        } catch (Exception e) {
            logger.warning("Задача после shutdown отклонена: " + e.getMessage());
        }

        Thread.sleep(5000);
    }
}