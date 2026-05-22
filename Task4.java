/**
 * Задача: Шагающий Робот (Strict Alternation)
 * Техническое задание:
 * У вас есть Робот, у которого есть две "ноги" — левая и правая.
 * Каждая нога управляется своим собственным отдельным потоком (Thread).
 * Вам нужно заставить робота сделать 10 шагов (то есть вывести в консоль слова "Левая" и "Правая").
 * <p>
 * Главное условие: Потоки запускаются одновременно, но они обязаны работать строго по очереди.
 * Два раза подряд одна и та же нога шагнуть не может. Вывод в консоли должен быть идеальным:
 * Левая
 * Правая
 * Левая
 * Правая
 * ...и так далее
 */
public class Task4 {

    // 1. Общий объект-монитор (замок), по которому потоки будут синхронизироваться
    private static final Object lock = new Object();
    private static boolean isLeftTurn = true;

    public static void main(String[] args) {

        Thread leftLeg = new Thread(() -> {
            for (int i = 0; i < 5; i++) { // Каждая нога делает по 5 шагов, суммарно 10
                synchronized (lock) {
                    while (!isLeftTurn) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    System.out.println("Левая");
                    isLeftTurn = false;
                    lock.notify();
                }
            }
        });
        Thread rightLeg = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                synchronized (lock) {
//                     Пока НЕ очередь правой ноги (то есть isLeftTurn == true) — ждем
                    while (isLeftTurn) {
                        try {
                            lock.wait(); // Поток засыпает и отпускает монитор lock
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    System.out.println("Правая");
                    isLeftTurn = true;
                    lock.notify();
                }
            }
        });
        leftLeg.start();
        rightLeg.start();
    }
}
