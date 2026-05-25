import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

class Wallet {
    private int amount;
    private final int id; // ID нужен для порядка блокировки

    public Wallet(int id, int amount) {
        this.id = id;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "amount=" + amount +
                ", id=" + id +
                '}';
    }

    // Метод перевода
    public static void transfer(Wallet from, Wallet to, int value) {
        // ВАЖНО: Блокируем кошельки в строгом порядке (по ID)
        // Это предотвращает Deadlock (взаимную блокировку)
        Wallet firstLock = from.id < to.id ? from : to;
        Wallet secondLock = from.id < to.id ? to : from;

        synchronized (firstLock) {
            synchronized (secondLock) {
                if (from.amount >= value) {
                    from.amount -= value;
                    to.amount += value;
                }
            }
        }
    }

    public int getAmount() {
        return amount;
    }

    public int getId() {
        return id;
    }
}

// Создаем класс задачи, которая умеет запускаться в отдельном потоке
class TransferTask extends Thread {
    private final Wallet from;
    private final Wallet to;
    private final int amount;

    public TransferTask(Wallet from, Wallet to, int amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    @Override
    public void run() {
        // Этот код выполнится в отдельном потоке
        System.out.println(Thread.currentThread().getName() + " transfer " + amount + " from " + from + " to " + to);
        Wallet.transfer(from, to, amount);
    }
}

//решение на потоках
class Main {
    public static void main(String[] args) throws InterruptedException {
        Wallet[] wallets = new Wallet[10];
        for (int i = 0; i < 10; i++) {
            wallets[i] = new Wallet(i, 100);
        }

        //Создаем список потоков, чтобы потом дождаться их завершения
        List<Thread> threads = new ArrayList<>();

        // Запускаем 1000 задач (переводы по кругу)
        for (int i = 0; i < 100; i++) {
            final int fromId = i % 10;
            final int toId = (i + 1) % 10; // Следующий по кругу, 9->0
            Thread task = new TransferTask(wallets[fromId], wallets[toId], 100);
            threads.add(task); // добавляем в список для ожидания
            task.start();
        }

        for (Thread thread : threads) {
            thread.join(); // Приостанавливает main, пока thread не умрет
        }

        int total = Arrays.stream(wallets).mapToInt(Wallet::getAmount).sum();
        System.out.println("Общая сумма: " + total); // Должно быть 1000

        Arrays.stream(wallets).forEach(System.out::println);
    }
}

//решение на пуле потоков
class WalletSystem {

    public static void main(String[] args) {
        // Создаем пул фиксированного размера (100 потоков)
        ExecutorService executor = Executors.newFixedThreadPool(100);

        // 2. Создаем 10 кошельков (ID от 0 до 9) с 100 руб каждый
        WalletLock[] wallets = new WalletLock[10];
        for (int i = 0; i < 10; i++) {
            wallets[i] = new WalletLock(i, 100);
        }

        // 3. Запускаем 1000 задач (переводы по кругу)
        for (int i = 0; i < 100; i++) {
            final int fromId = i % 10;
            final int toId = (i + 1) % 10; // Следующий по кругу, 9->0

            executor.submit(() -> {
                boolean success = false;
                while (!success) {
                    success = WalletSystem.transfer(wallets[fromId], wallets[toId], 100);
                    if (!success) {
                        // Если не удалось захватить локи, даем шанс другим потокам
                        // и пробуем снова чуть позже.
                        // В реальной системе здесь лучше сделать небольшую паузу (backoff)
                        Thread.yield();
                    }
                }
            });
        }

        // Ждем завершения и проверяем сумму
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        int total = 0;
        for (WalletLock w : wallets) {
            System.out.println("Wallet " + w.getId() + ": " + w.getAmount());
            total += w.getAmount();
        }
        System.out.println("TOTAL: " + total); // Всегда будет 1000

    }

    // Метод перевода с использованием tryLock (без блокирования потоков)
    public static boolean transfer(WalletLock from, WalletLock to, int amount) {
        // Сортируем порядок блокировки по ID (чтобы избежать Deadlock)
        WalletLock first = from.getId() < to.getId() ? from : to;
        WalletLock second = from.getId() < to.getId() ? to : from;

        // tryLock() пытается захватить монитор.
        // Если занято - возвращает false сразу, НЕ зависая в ожидании.
        // Это ключевое отличие от synchronized!

        if (first.lock.tryLock()) {
            try {
                if (second.lock.tryLock()) {
                    try {
                        // Критическая секция: оба кошелька захвачены нами
                        if (from.amount >= amount) {
                            from.amount -= amount;
                            to.amount += amount;
                            System.out.println(Thread.currentThread().getName() + " transfer " + amount + " from " + from + " to " + to + " success");

                            return true; // Успех
                        }
                    } finally {
                        second.lock.unlock();
                    }
                }
            } finally {

                first.lock.unlock();
            }
        }
        System.out.println(Thread.currentThread().getName() + " transfer " + amount + " from " + from + " to " + to + " failed");
        return false; // Не удалось захватить локи (кто-то другой занят)
    }
}

class WalletLock {
    int id;
    int amount;
    ReentrantLock lock = new ReentrantLock(); // Каждый кошелек имеет свой замок

    @Override
    public String toString() {
        return "WalletLock{" +
                "id=" + id +
                ", amount=" + amount +
                '}';
    }

    public WalletLock(int id, int amount) {
        this.id = id;
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public int getId() {
        return id;
    }
}