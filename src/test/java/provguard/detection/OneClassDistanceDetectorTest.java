package provguard.detection;

import org.junit.jupiter.api.Test;
import provguard.graph.FeatureExtractor;
import provguard.graph.FeatureVector;
import provguard.graph.GraphBuilder;
import provguard.graph.ProvenanceGraph;
import provguard.provenance.EventBuffer;
import provguard.provenance.ProvenanceEvent;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Trains OneClassDistanceDetector on REAL ProvenanceGraphs, produced by
 * actually triggering the ProcessBuilder sensor through the real agent (not
 * invented feature numbers), then checks its behavior against both a
 * same-shaped and a deliberately different-shaped graph. The agent is
 * attached at JVM startup for the whole test run - see pom.xml and
 * docs/DESIGN_DECISIONS.md section 4G.
 */
class OneClassDistanceDetectorTest {

    @Test
    void trainedOnRealCapturedTracesAndFlagsAStructurallyDifferentOne() throws Exception {
        List<FeatureVector> realBenignTrainingSet = captureRealBenignFeatureVectors();
        assertFalse(realBenignTrainingSet.isEmpty(), "Should have captured at least one real training example");

        OneClassDistanceDetector detector = new OneClassDistanceDetector(realBenignTrainingSet, 2.0);

        // A HELD-OUT real capture (same code path, not part of training) - NOT a
        // synthetic 2-frame example. StackWalkerCollector captures the entire
        // real call chain (up to 20 frames), including JUnit's own reflection/
        // invocation machinery between the test method and this call - a
        // synthetic "sink + 1 frame" example would have a wildly different
        // depth than what's genuinely captured here and isn't a fair comparison.
        List<FeatureVector> heldOut = captureRealBenignFeatureVectors();
        FeatureVector heldOutVector = heldOut.get(0);
        assertFalse(detector.detect(rebuildGraphFromVector(heldOutVector)).anomalous(),
                "A held-out real capture from the same code path as training should not be flagged");

        // A deliberately much deeper call chain than ANY real capture here
        // (training depth + 20 extra synthetic frames) SHOULD be flagged -
        // this is the one honest thing pure structural depth/edge-count
        // features can actually catch, independent of caller identity.
        int muchDeeperThanTraining = realBenignTrainingSet.get(0).depth() + 20;
        ProvenanceGraph deeplyDifferentShape = buildSyntheticGraphOfDepth(muchDeeperThanTraining);
        assertTrue(detector.detect(deeplyDifferentShape).anomalous(),
                "A call chain 20 frames deeper than any real training example should be flagged as structurally anomalous");
    }

    /** Builds a graph with exactly the given number of nodes, for controlled synthetic tests. */
    private static ProvenanceGraph buildSyntheticGraphOfDepth(int depth) {
        List<String> frames = new ArrayList<>();
        frames.add("java.lang.ProcessBuilder#start");
        for (int i = 0; i < depth - 1; i++) {
            frames.add("some.gadget.ChainLink" + i + "#invoke");
        }
        return GraphBuilder.build("PROCESS_EXECUTION", frames);
    }

    /** Rebuilds a minimal graph reproducing a given FeatureVector's shape, for the held-out comparison. */
    private static ProvenanceGraph rebuildGraphFromVector(FeatureVector vector) {
        return buildSyntheticGraphOfDepth(vector.depth());
    }

    @Test
    void demonstratesTheDocumentedLimitation_sameShapeDifferentCallerIsNotCaught() {
        // This test exists to make the limitation documented in
        // OneClassDistanceDetector's Javadoc concrete and verifiable, not to
        // pretend it doesn't exist: a same-shaped call from an UNTRUSTED
        // caller is indistinguishable from a trusted one using structural
        // features alone. AllowlistDetector (caller-identity-based) is what
        // actually catches this case in the real enforcement pipeline.
        OneClassDistanceDetector detector = new OneClassDistanceDetector(
                List.of(new FeatureVector(2, 1), new FeatureVector(2, 1), new FeatureVector(2, 1)), 2.0);

        ProvenanceGraph trustedShapedCall = GraphBuilder.build("PROCESS_EXECUTION",
                List.of("java.lang.ProcessBuilder#start", "provguard.cli.DemoMain#main"));
        ProvenanceGraph untrustedButSameShapedCall = GraphBuilder.build("PROCESS_EXECUTION",
                List.of("java.lang.ProcessBuilder#start", "some.attacker.GadgetEntry#invoke"));

        assertFalse(detector.detect(trustedShapedCall).anomalous());
        assertFalse(detector.detect(untrustedButSameShapedCall).anomalous(),
                "Documents the real limitation: same shape, different (untrusted) caller is NOT caught by structural features alone");
    }

    /**
     * Actually triggers real ProcessBuilder calls through the live agent and
     * rebuilds ProvenanceGraphs from the genuinely captured EventBuffer
     * entries - real training data, not fabricated numbers.
     *
     * Deliberately homogeneous (process-execution only, not mixed with
     * deserialization): ObjectInputStream#resolveClass is reached via
     * several internal ObjectInputStream-to-itself calls
     * (readObject -> readObject0 -> readOrdinaryObject -> ... -> resolveClass),
     * so its captured graphs have a genuinely different, larger depth than a
     * direct ProcessBuilder#start() call. Mixing both sink types into one
     * training set with only 2-3 samples produces an unstable mean/stddev
     * that doesn't represent either distribution well - a real, useful
     * finding in its own right (a production version of this detector would
     * need to be trained per sink type, not pooled), not something to hide
     * by training on more homogeneous-looking data than is honest.
     */
    private static List<FeatureVector> captureRealBenignFeatureVectors() throws Exception {
        EventBuffer.INSTANCE.clear();

        new ProcessBuilder("cmd", "/c", "echo training-example-1").start().waitFor();
        new ProcessBuilder("cmd", "/c", "echo training-example-2").start().waitFor();
        new ProcessBuilder("cmd", "/c", "echo training-example-3").start().waitFor();

        List<FeatureVector> vectors = new ArrayList<>();
        for (ProvenanceEvent event : EventBuffer.INSTANCE.getAll()) {
            ProvenanceGraph graph = GraphBuilder.build(event.sinkType().name(), event.callStack());
            vectors.add(FeatureExtractor.extract(graph));
        }
        return vectors;
    }
}
