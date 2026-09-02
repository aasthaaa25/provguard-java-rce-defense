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

    private static final int MAX_FRAMES = 20;

    private StackWalkerCollector() {
    }

    public static List<String> captureCallStack() {
        return WALKER.walk(frames -> frames
                .limit(MAX_FRAMES)
                .map(f -> f.getClassName() + "#" + f.getMethodName())
                .collect(Collectors.toList()));
    }
}
