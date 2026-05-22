import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Задача 2: Пул потоков фиксированного размера (Средний уровень)
 * Техническое задание:
 * Представьте, что у вас есть список из 10 тяжелых задач (например, симулирующих скачивание картинок).
 * Каждая задача просто спит 1 секунду (Thread.sleep(1000)) и выводит в консоль "Картинка X скачана потоком Y".
 * Главное условие: Вам нужно обработать эти 10 задач, используя пул потоков фиксированного размера
 * (ровно 3 потока). Напишите код так, чтобы одновременно выполнялось не более 3 задач,
 * а остальные ждали своей очереди. В конце программа должна корректно завершить работу пула.
 * <p>
 * Подсказка:
 * <p>
 * Не создавайте потоки вручную через new Thread.
 * Используйте фабрику исполнителей Executors.newFixedThreadPool(3).
 * Передавайте задачи в пул через метод submit() или execute(),
 * а в конце обязательно вызовите shutdown().
 */
public class Task2 {
    public static void main(String[] args) {

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (int i = 1; i <= 10; i++) {
            final int index = i;
            executorService.execute(() ->
            {
                System.out.println("Картинка " + index + " начала скачиваться потоком " + Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Картинка " + index + " скачана потоком " + Thread.currentThread().getName());

            });
        }
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();

                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("Пул потоков не был корректно остановлен");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

