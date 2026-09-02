package provguard.runtime;

import provguard.enforcement.Decision;
import provguard.provenance.ProvenanceEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronously persists provenance events + decisions to a local trace file,
 * entirely off the thread that triggered the sink.
 *
 * WHY VIRTUAL THREADS SPECIFICALLY: the enforcement decision itself
 * (AllowlistDetector -> PolicyEngine -> EnforcementEngine, all synchronous,
 * in SensorPipeline) MUST stay synchronous - BLOCK mode only works because it
 * throws before the sink's real body runs, and that requires the decision to
 * already be made by the time control would return to the sink. Tracing,
 * by contrast, has no such constraint: by the time writeAsync() is called,
 * the ALLOW/LOG/BLOCK decision has already happened. Writing it to disk is
 * pure I/O with no reason to make the calling (possibly app) thread wait,
 * and a burst of sink hits could otherwise exhaust a bounded platform-thread
 * pool - exactly the scenario virtual threads (one per submitted task, no
 * fixed pool to exhaust) are for.
 *
 * Disabled by default (see provguard.enforcement.Policy#tracingEnabled) so
 * existing tests and demos don't silently start writing files; opt-in via
 * that flag, and see TraceWriterTest for a test that actually enables it and
 * reads the file back.
 */
public final class TraceWriter {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private TraceWriter() {
    }

    public static void writeAsync(Path traceFile, ProvenanceEvent event, Decision decision) {
        EXECUTOR.submit(() -> {
            try {
                String line = formatLine(event, decision);
                Files.writeString(traceFile, line + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                // Best-effort: a tracing failure must never crash or block the
                // monitored application. Log and move on.
                System.err.println("[ProvGuard] failed to write trace: " + e.getMessage());
            }
        });
    }

    public static Path defaultTraceFile() {
        return Paths.get(System.getProperty("provguard.trace.file", "provguard-trace.log"));
    }

    private static String formatLine(ProvenanceEvent event, Decision decision) {
        String decisionLabel = switch (decision) {
            case Decision.Allow ignored -> "ALLOW";
            case Decision.Log log -> "LOG(" + log.reason() + ")";
            case Decision.Block block -> "BLOCK(" + block.reason() + ")";
        };
        return "%s\t%s\t%s\t%s".formatted(
                event.timestamp(), event.sinkType(), decisionLabel, event.callStack());
    }
}
