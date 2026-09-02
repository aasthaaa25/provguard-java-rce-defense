package provguard.tools;

import provguard.graph.FeatureExtractor;
import provguard.graph.FeatureVector;
import provguard.graph.GraphBuilder;
import provguard.graph.ProvenanceGraph;
import provguard.provenance.EventBuffer;
import provguard.provenance.ProvenanceEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Generates the local dataset under dataset/benign and dataset/malicious by
 * ACTUALLY TRIGGERING real sink calls through the live ProvGuard agent and
 * recording the REAL captured ProvenanceEvents/features - not fabricated
 * numbers.
 *
 * IMPORTANT HONESTY NOTE (see dataset/README.md and docs/DATASET.md): the
 * "malicious" data here is NOT from a real exploit chain (no ysoserial or
 * similar payload is used or bundled anywhere in this project). It is
 * "malicious-shaped" in exactly one specific sense: it comes from a caller
 * class deliberately absent from provguard.enforcement.Policy's trusted
 * allowlist, structurally identical in most respects to the benign calls.
 * This is enough to exercise AllowlistDetector meaningfully, but is a real,
 * stated limitation for anything claiming to evaluate detection of actual
 * gadget chains - see docs/EVALUATION.md.
 *
 * Run (from the project root, after `mvn package`):
 *   java -javaagent:target/provguard-agent.jar -cp target/provguard-agent.jar provguard.tools.DatasetGenerator
 */
public final class DatasetGenerator {

    private static final int BENIGN_SAMPLES_PER_SINK = 20;
    private static final int MALICIOUS_SAMPLES_PER_SINK = 20;

    private DatasetGenerator() {
    }

    public static void main(String[] args) throws Exception {
        List<ProvenanceEvent> benignEvents = collectBenignTraces();
        List<ProvenanceEvent> maliciousEvents = collectMaliciousShapedTraces();

        writeDataset(Paths.get("dataset/benign/local-benign-traces.jsonl"), benignEvents, "benign");
        writeDataset(Paths.get("dataset/malicious/local-malicious-shaped-traces.jsonl"), maliciousEvents, "malicious_shaped_local_synthetic");

        System.out.println("Wrote " + benignEvents.size() + " benign trace(s) and "
                + maliciousEvents.size() + " malicious-shaped trace(s).");
    }

    /** Trusted caller (this class is on Policy's allowlist) - genuinely allowed, real captures. */
    private static List<ProvenanceEvent> collectBenignTraces() throws Exception {
        EventBuffer.INSTANCE.clear();

        for (int i = 0; i < BENIGN_SAMPLES_PER_SINK; i++) {
            new ProcessBuilder("cmd", "/c", "echo benign-sample-" + i).start().waitFor();
        }
        for (int i = 0; i < BENIGN_SAMPLES_PER_SINK; i++) {
            deserializeSample();
        }

        return EventBuffer.INSTANCE.getAll();
    }

    /**
     * Untrusted caller (NOT on Policy's allowlist) - every call here gets
     * BLOCKED by real enforcement, but SensorPipeline records the
     * ProvenanceEvent BEFORE calling EnforcementEngine (see
     * SensorPipeline#captureAndEnforce), so the real captured data survives
     * even though the underlying sink call itself never completes.
     */
    private static List<ProvenanceEvent> collectMaliciousShapedTraces() throws Exception {
        EventBuffer.INSTANCE.clear();
        UntrustedSampleSource untrusted = new UntrustedSampleSource();

        for (int i = 0; i < MALICIOUS_SAMPLES_PER_SINK; i++) {
            try {
                untrusted.runProcess(i);
            } catch (SecurityException blocked) {
                // expected - see class Javadoc
            }
        }
        for (int i = 0; i < MALICIOUS_SAMPLES_PER_SINK; i++) {
            try {
                untrusted.runDeserialization();
            } catch (SecurityException blocked) {
                // expected - see class Javadoc
            }
        }

        return EventBuffer.INSTANCE.getAll();
    }

    private static void deserializeSample() throws IOException, ClassNotFoundException {
        byte[] payload = serialize(new SamplePayload());
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(payload))) {
            in.readObject();
        }
    }

    private static byte[] serialize(Object value) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
            return bytes.toByteArray();
        }
    }

    private static void writeDataset(Path file, List<ProvenanceEvent> events, String label) throws IOException {
        Files.createDirectories(file.getParent());
        StringBuilder content = new StringBuilder();
        for (ProvenanceEvent event : events) {
            ProvenanceGraph graph = GraphBuilder.build(event.sinkType().name(), event.callStack());
            FeatureVector features = FeatureExtractor.extract(graph);
            content.append(toJsonLine(event, graph, features, label)).append('\n');
        }
        Files.writeString(file, content.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String toJsonLine(ProvenanceEvent event, ProvenanceGraph graph, FeatureVector features, String label) {
        String caller = graph.callerClassName();
        return """
                {"timestamp":"%s","sinkType":"%s","label":"%s","depth":%d,"edgeCount":%d,"caller":%s}"""
                .formatted(
                        event.timestamp(),
                        event.sinkType(),
                        label,
                        features.depth(),
                        features.edgeCount(),
                        caller == null ? "null" : "\"" + caller.replace("\"", "\\\"") + "\""
                );
    }

    /** Deliberately absent from Policy's trusted allowlist. */
    static final class UntrustedSampleSource {
        void runProcess(int i) throws Exception {
            new ProcessBuilder("cmd", "/c", "echo malicious-shaped-sample-" + i).start();
        }

        void runDeserialization() throws Exception {
            byte[] payload = serialize(new SamplePayload());
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(payload))) {
                in.readObject();
            }
        }
    }

    static final class SamplePayload implements Serializable {
        private static final long serialVersionUID = 1L;
        final String value = "dataset-generator-payload";
    }
}
