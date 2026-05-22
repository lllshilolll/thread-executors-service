import java.util.concurrent.*;

/**
 * Задача 3: Пул с динамическим расписанием (Крон-джоба) (Средний уровень)
 * Техническое задание:
 * Реализуйте симуляцию аннотации @Scheduled.
 * Напишите фоновую задачу, которая должна запускаться каждые 2 секунды,
 * выполнять "логирование системы" (выводить в консоль текущее время и фразу "Лог записан")
 * и работать на протяжении 10 секунд, после чего вся программа должна автоматически и плавно выключаться.
 */
public class Task3 {
    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            System.out.println("логирование системы потоком " + Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Лог записан " + Thread.currentThread().getName());
        };
        ScheduledFuture<?> cron =   executor.scheduleAtFixedRate(task, 0, 2, TimeUnit.SECONDS);

        Runnable shutdownTask = () -> {
            System.out.println("10 секунд прошло. Начинаем плавное выключение...");
            cron.cancel(false);

            executor.shutdown();

            try{
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)){
                    executor.shutdownNow();
                }
                System.out.println("Программа успешно и плавно завершена.");
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        };

        executor.schedule(shutdownTask, 10, TimeUnit.SECONDS);
    }
}
