# Custom Thread Pool

## Описание
Реализация кастомного пула потоков с расширенными возможностями.

## Функциональность
- Настраиваемый размер пула (corePoolSize)
- Очередь задач с ограничением (queueSize)
- Политика отказа AbortPolicy
- Детальное логирование всех событий
- Поддержка execute(Runnable) и submit(Callable)

## Использование
```java
CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor.Builder()
    .corePoolSize(4)
    .queueSize(10)
    .poolName("my-pool")
    .build();

pool.execute(() -> System.out.println("Task executed"));
Future<String> future = pool.submit(() -> "Result");
pool.shutdown();
Сборка и запуск
bash
mvn clean compile
mvn test
mvn exec:java -Dexec.mainClass="com.concurrent.pool.Main"
Тесты
testExecute - проверка выполнения задач

testSubmit - проверка Future результатов

testMultipleTasks - проверка множества задач

Результаты
✅ Все тесты пройдены успешно
