import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

/**
 * TOPIC: virtual threads (Project Loom, JEP 444).
 *
 * WHY IT MATTERS: ProvGuard's design calls for running detection work on
 * virtual threads specifically so a burst of sink hits never exhausts a
 * bounded platform-thread pool and starts blocking application threads.
 */
public class VirtualThreadsDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("Platform thread count is expensive to scale; virtual threads are cheap.");

        // newVirtualThreadPerTaskExecutor: one virtual thread per submitted task,
        // no fixed pool size to exhaust.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> futures = IntStream.range(0, 20)
                    .mapToObj(i -> executor.submit(() -> {
                        // Simulate a short bit of "detection" work per sink hit.
                        Thread.sleep(10);
                        return i * i;
                    }))
                    .toList();

            int total = 0;
            for (Future<Integer> f : futures) {
                total += f.get();
            }
            System.out.println("processed " + futures.size() + " tasks concurrently, total=" + total);
        }

        // A single virtual thread started directly.
        Thread vt = Thread.ofVirtual().name("provguard-detector-vt").start(() -> {
            System.out.println("running on: " + Thread.currentThread());
        });
        vt.join();
    }
}
