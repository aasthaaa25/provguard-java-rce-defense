import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TOPIC: Thread/Runnable, ExecutorService, Future, synchronized vs
 * ReentrantLock, AtomicInteger, volatile.
 *
 * WHY IT MATTERS: ProvGuard's detection must run off the application's hot
 * path (see docs/ARCHITECTURE.md) - this is the toolbox for that.
 */
public class ConcurrencyDemo {

    // volatile: guarantees visibility of writes across threads (no caching in
    // a thread-local register), but does NOT make check-then-act atomic.
    static volatile boolean shuttingDown = false;

    // AtomicInteger: lock-free, safe increment from multiple threads.
    static final AtomicInteger sinkHitCount = new AtomicInteger();

    // A plain object monitor lock (synchronized) vs an explicit ReentrantLock -
    // shown side by side so the tradeoffs are visible in one place.
    static final Object monitor = new Object();
    static int synchronizedCounter = 0;

    static final ReentrantLock lock = new ReentrantLock();
    static int lockCounter = 0;

    public static void main(String[] args) throws Exception {
        // Runnable + Thread, the lowest-level building block.
        Thread watcher = new Thread(() -> {
            while (!shuttingDown) {
                // busy-poll briefly, just for the demo
            }
            System.out.println("watcher thread saw shutdown flag");
        }, "watcher");
        watcher.start();

        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            // Submit tasks that increment shared counters via different strategies.
            Runnable incrementAll = () -> {
                sinkHitCount.incrementAndGet();

                synchronized (monitor) {
                    synchronizedCounter++;
                }

                lock.lock();
                try {
                    lockCounter++;
                } finally {
                    lock.unlock();
                }
            };

            for (int i = 0; i < 100; i++) {
                pool.submit(incrementAll);
            }

            // Future: get a result back from an async computation.
            Future<Integer> future = pool.submit(() -> 21 * 2);
            System.out.println("future result: " + future.get());

            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }

        shuttingDown = true;
        watcher.join();

        System.out.println("sinkHitCount (Atomic): " + sinkHitCount.get());
        System.out.println("synchronizedCounter:  " + synchronizedCounter);
        System.out.println("lockCounter:          " + lockCounter);
    }
}
