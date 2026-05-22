import java.util.List;
import java.util.ArrayList;

/**
 * Задача 1: Ручное создание потоков и вывод по порядку (Легкий уровень)
 * Техническое задание:
 * Создайте программу, которая запускает 3 независимых потока.
 * Каждый поток должен последовательно вывести числа от 1 до 5,
 * а также своё имя (например, Thread-1, Thread-2 и т.д.).
 * Главное условие: Основной поток программы (main) должен дождаться завершения всех трёх потоков
 * и только после этого вывести в консоль финальную фразу: "Все потоки завершили работу".
 */
public class Task1 {
    public static void main(String[] args) {
        List<Thread> threads = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            Thread thread = new Thread(new CustomThread(), "Thread-" + i);
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.err.println("Ожидание прервано: " + e.getMessage());
                Thread.currentThread().interrupt();
                return;
            }
        }

        System.out.println("Все потоки завершили работу");
    }
}

class CustomThread implements Runnable {

    @Override
    public void run() {
        for (int i = 1; i <= 5; i++) {
            System.out.println(Thread.currentThread().getName() + " : " + i);
        }
    }
}
