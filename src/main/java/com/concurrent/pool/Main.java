package com.concurrent.pool;

import java.util.concurrent.TimeUnit;
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

        // Тест 3: Множество задач
        testMultipleTasks();

        // Тест 4: Shutdown и завершение
        testShutdown();
    }

    private static void testBasicFunctionality() throws InterruptedException {
        logger.info("\n--- Тест 1: Базовая работа пула ---");

        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
                .corePoolSize(2)
                .queueSize(3)
                .poolName("basic")
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

        Thread.sleep(8000); // Ждем выполнения
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
    }

    private static void testOverloadHandling() throws InterruptedException {
        logger.info("\n--- Тест 2: Обработка перегрузки ---");

        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
                .corePoolSize(1)
                .queueSize(1)
                .poolName("overload")
                .rejectedHandler(new AbortPolicy())
                .build();

        // Первая задача займет поток
        pool.execute(() -> {
            try {
                logger.info("=== Долгая задача началась");
                Thread.sleep(3000);
                logger.info("=== Долгая задача завершилась");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(100); // Даем время первой задаче начаться

        // Вторая задача пойдет в очередь
        pool.execute(() -> {
            logger.info("=== Задача в очереди выполняется");
        });

        // Третья задача должна быть отклонена
        try {
            pool.execute(() -> {
                logger.info("Эта задача не должна выполниться");
            });
        } catch (Exception e) {
            logger.warning("!!! Задача отклонена: " + e.getMessage());
        }

        Thread.sleep(4000);
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
    }

    private static void testMultipleTasks() throws InterruptedException {
        logger.info("\n--- Тест 3: Множество задач ---");

        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
                .corePoolSize(3)
                .queueSize(10)
                .poolName("multi")
                .build();

        // Отправляем 10 задач
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            pool.execute(() -> {
                try {
                    logger.info("=== Задача " + taskId + " выполняется");
                    Thread.sleep(500);
                    logger.info("=== Задача " + taskId + " завершена");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            Thread.sleep(50);
        }

        Thread.sleep(6000);
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
    }

    private static void testShutdown() throws InterruptedException {
        logger.info("\n--- Тест 4: Проверка shutdown ---");

        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
                .corePoolSize(2)
                .queueSize(5)
                .poolName("shutdown")
                .build();

        // Отправляем задачи
        for (int i = 1; i <= 4; i++) {
            final int taskId = i;
            pool.execute(() -> {
                try {
                    logger.info("=== Задача " + taskId + " старт");
                    Thread.sleep(2000);
                    logger.info("=== Задача " + taskId + " финиш");
                } catch (InterruptedException e) {
                    logger.info("=== Задача " + taskId + " прервана");
                }
            });
        }

        Thread.sleep(500);
        logger.info("Вызываем shutdown...");
        pool.shutdown();

        // Пытаемся добавить новую задачу после shutdown
        try {
            pool.execute(() -> logger.info("Эта задача не должна выполниться"));
        } catch (Exception e) {
            logger.warning("Задача после shutdown отклонена: " + e.getMessage());
        }

        pool.awaitTermination(5, TimeUnit.SECONDS);
        logger.info("Пул полностью остановлен");
    }
}