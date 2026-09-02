import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * TOPIC: CompletableFuture composition (thenApply, thenCombine, exceptionally,
 * async variants).
 *
 * WHY IT MATTERS: ProvGuard's detection pipeline (capture -> featurize ->
 * score) is a natural fit for chained async stages that never block the
 * thread that triggered the sink.
 */
public class CompletableFutureDemo {

    record ProvenanceEventLite(String sinkType) {}
    record FeatureVector(int length) {}
    record Score(double value) {}

    static CompletableFuture<ProvenanceEventLite> capture() {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("capturing on thread: " + Thread.currentThread().getName());
            return new ProvenanceEventLite("PROCESS_EXECUTION");
        });
    }

    static FeatureVector featurize(ProvenanceEventLite event) {
        return new FeatureVector(event.sinkType().length());
    }

    static Score score(FeatureVector features) {
        return new Score(features.length() * 0.1);
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<Score> pipeline = capture()
                .thenApply(CompletableFutureDemo::featurize) // stage 2, chained
                .thenApply(CompletableFutureDemo::score)      // stage 3, chained
                .exceptionally(ex -> {
                    System.out.println("pipeline failed: " + ex.getMessage());
                    return new Score(-1);
                });

        System.out.println("main thread continues immediately: " + Thread.currentThread().getName());
        System.out.println("final score: " + pipeline.get());

        // thenCombine: merge two independent async computations.
        CompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> 10);
        CompletableFuture<Integer> b = CompletableFuture.supplyAsync(() -> 32);
        CompletableFuture<Integer> combined = a.thenCombine(b, Integer::sum);
        System.out.println("combined: " + combined.get());
    }
}
