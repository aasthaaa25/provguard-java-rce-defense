package provguard.provenance;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Captures the current call stack using {@link StackWalker} rather than the
 * older {@code Thread.currentThread().getStackTrace()} / {@code new Throwable()}
 * approaches, because StackWalker walks frames lazily and avoids eagerly
 * materializing a full Throwable just to read frame metadata — the lower-overhead
 * option explicitly called for in the ProvGuard design.
 */
public final class StackWalkerCollector {

    private static final StackWalker WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    // Lowered from 20 during the overhead investigation in
    // evaluation/results/deserialization-overhead-2026-09-03.md: AllowlistDetector
    // and OneClassDistanceDetector only ever look at the sink frame plus the
    // first differently-classed caller frame (ProvenanceGraph#callerClassName),
    // so capturing 20 frames on every single sink hit was doing (and allocating
    // GraphNode/GraphEdge objects for) far more work than anything downstream
    // actually uses. 8 is enough headroom for self-recursive sinks like
    // ObjectInputStream#resolveClass (readObject -> ... -> resolveClass is a
    // handful of frames) while still being meaningfully cheaper than 20.
    private static final int MAX_FRAMES = 15;

    private StackWalkerCollector() {
    }

    public static List<String> captureCallStack() {
        return WALKER.walk(frames -> frames
                .limit(MAX_FRAMES)
                .map(f -> f.getClassName() + "#" + f.getMethodName())
                .collect(Collectors.toList()));
    }
}
