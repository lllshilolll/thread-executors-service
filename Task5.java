import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Задача 5: Продюсер-Консьюмер (Producer-Consumer) на пулах (Профессиональный уровень)
 * Техническое задание:
 * Реализуйте классический паттерн взаимодействия потоков через общую очередь.
 * У вас есть общая потокобезопасная очередь (буфер) ограниченного размера (например, максимум 5 элементов).
 * <p>
 * Создайте пул "Продюсеров" (2 потока), которые непрерывно генерируют случайные числа (задачи)
 * и кладут их в очередь. Если очередь заполнена, они должны ждать.
 * <p>
 * Создайте пул "Консьюмеров" (3 потока), которые непрерывно забирают числа из очереди
 * и "обрабатывают" их (выводят в консоль). Если очередь пуста, они должны ждать появления элементов.
 */
public class Task5 {

    public static void main(String[] args) {
        var producers = Executors.newFixedThreadPool(2);
        var consumers = Executors.newFixedThreadPool(3);
        Random random = new Random();
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);
        for (int i = 0; i < 2; i++) {
            producers.execute(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        var value = random.nextInt(100);
                        queue.put(value);
                        System.out.println("producer " + Thread.currentThread().getName() + " положил " + value);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

        for (int i = 0; i < 3; i++) {

            consumers.execute(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        var value = queue.take();
                        System.out.println("consumer " + Thread.currentThread().getName() + " вычитал " + value);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("=== Начинаем остановку программы ===");

        // === 2. Жестко прерываем потоки через shutdownNow ===
        // Это посылает сигнал interrupt() во все потоки пула
        producers.shutdownNow();
        consumers.shutdownNow();

        try {
            // Ждем максимум 2 секунды, пока потоки обработают InterruptedException и выйдут из run()
            boolean producersDead = producers.awaitTermination(2, TimeUnit.SECONDS);
            boolean consumersDead = consumers.awaitTermination(2, TimeUnit.SECONDS);

            if (producersDead && consumersDead) {
                System.out.println("=== Все потоки успешно и мягко остановлены. Программа завершена ===");
            } else {
                System.err.println("=== Внимание: некоторые потоки зависли и не успели закрыться ===");
                producers.shutdownNow();
                consumers.shutdownNow();
            }
        } catch (InterruptedException e) {
            producers.shutdownNow();
            consumers.shutdownNow();
            System.out.println("Ожидание завершения пулов было прервано");
            Thread.currentThread().interrupt();
        }
    }
}
