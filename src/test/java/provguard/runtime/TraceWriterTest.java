package provguard.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import provguard.enforcement.Policy;
import provguard.provenance.EventBuffer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Actually enables tracing, triggers a real ProcessBuilder call through the
 * live agent, and reads the ACTUAL file TraceWriter wrote back from disk -
 * not a mock of the writer, the real async I/O path.
 */
class TraceWriterTest {

    private Path traceFile;

    @BeforeEach
    void setUp() throws IOException {
        EventBuffer.INSTANCE.clear();
        traceFile = Files.createTempFile("provguard-trace-test-", ".log");
        Files.deleteIfExists(traceFile);
        System.setProperty("provguard.trace.file", traceFile.toString());
        Policy.tracingEnabled = true;
    }

    @AfterEach
    void tearDown() throws IOException {
        Policy.tracingEnabled = false;
        System.clearProperty("provguard.trace.file");
        Files.deleteIfExists(traceFile);
    }

    @Test
    void asyncTraceWriteActuallyLandsOnDisk() throws Exception {
        // TraceWriterTest is not on Policy's allowlist, so this call gets
        // BLOCKED - which is fine and expected: tracing fires before
        // enforcement (see SensorPipeline), so the trace line should exist
        // even for a blocked call.
        try {
            new ProcessBuilder("cmd", "/c", "echo trace-test").start();
        } catch (SecurityException expectedBlock) {
            // expected - see comment above
        }

        // The write is async (virtual thread); poll briefly for it to land
        // rather than assuming it's instantaneous.
        List<String> lines = waitForNonEmptyFile(traceFile, 2, TimeUnit.SECONDS);

        assertTrue(!lines.isEmpty(), "Expected at least one trace line to be written");
        assertTrue(lines.get(0).contains("PROCESS_EXECUTION"), "Expected the trace line to mention the sink type: " + lines.get(0));
        assertTrue(lines.get(0).contains("BLOCK"), "Expected the trace line to record the BLOCK decision: " + lines.get(0));
    }

    private static List<String> waitForNonEmptyFile(Path file, long timeout, TimeUnit unit) throws Exception {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            if (Files.exists(file) && Files.size(file) > 0) {
                return Files.readAllLines(file);
            }
            Thread.sleep(20);
        }
        return Files.exists(file) ? Files.readAllLines(file) : List.of();
    }
}
