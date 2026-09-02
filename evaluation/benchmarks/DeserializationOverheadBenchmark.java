import java.io.*;

/**
 * Measures REAL per-call overhead of ObjectInputStream#resolveClass()
 * deserialization, with and without the ProvGuard agent attached. Not a
 * rigorous JMH benchmark (no forked JVM per trial, no dead-code-elimination
 * guards beyond a warmup phase and printing the result) - but every number
 * it prints is an actual measurement (System.nanoTime() around real calls),
 * not an estimate.
 *
 * Deliberately benchmarks deserialization, not ProcessBuilder: ProcessBuilder#start()
 * spawns a real OS process, whose cost (milliseconds, dominated by OS
 * fork/exec) would completely swamp and hide any microsecond-scale overhead
 * our own Java-side instrumentation adds. Deserialization is pure in-JVM
 * work, so it actually isolates our overhead.
 *
 * Usage:
 *   javac DeserializationOverheadBenchmark.java
 *   java DeserializationOverheadBenchmark                                                        (baseline, no agent)
 *   java -Dnet.bytebuddy.experimental=true -javaagent:../../target/provguard-agent.jar DeserializationOverheadBenchmark   (with agent, monitoring only)
 *
 * This class is on ProvGuard's trusted allowlist (provguard.enforcement.Policy)
 * so the "with agent" run measures the realistic ALLOW-path cost (capture +
 * detect + decide), not the cost of throwing a SinkBlockedException on every
 * call, which would not be representative of normal operation.
 */
public class DeserializationOverheadBenchmark {

    private static final int WARMUP_ITERATIONS = 5_000;
    private static final int MEASURED_ITERATIONS = 50_000;

    public static void main(String[] args) throws Exception {
        byte[] payload = serialize(new Payload());

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            deserialize(payload);
        }

        long start = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            deserialize(payload);
        }
        long elapsedNanos = System.nanoTime() - start;

        double totalMillis = elapsedNanos / 1_000_000.0;
        double avgMicrosPerCall = (elapsedNanos / 1000.0) / MEASURED_ITERATIONS;

        System.out.println("=== DeserializationOverheadBenchmark ===");
        System.out.println("Warmup iterations:   " + WARMUP_ITERATIONS);
        System.out.println("Measured iterations: " + MEASURED_ITERATIONS);
        System.out.printf("Total time:           %.2f ms%n", totalMillis);
        System.out.printf("Average per call:     %.3f microseconds%n", avgMicrosPerCall);
    }

    private static byte[] serialize(Object value) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
            return bytes.toByteArray();
        }
    }

    private static Object deserialize(byte[] payload) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(payload))) {
            return in.readObject();
        }
    }

    static class Payload implements Serializable {
        private static final long serialVersionUID = 1L;
        final String value = "benchmark-payload";
    }
}
