package provguard.detection;

import org.junit.jupiter.api.Test;
import provguard.graph.GraphBuilder;
import provguard.graph.ProvenanceGraph;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The agent is attached at JVM startup for the whole test run (see pom.xml
 * and docs/DESIGN_DECISIONS.md section 4G) specifically so that pure unit
 * tests like this one - which reference provguard.graph/detection types
 * directly, with no sensor/agent code of their own - never risk being the
 * first thing in the JVM to load those types via the application classloader.
 */
class AllowlistDetectorTest {

    private final AnomalyDetector detector = new AllowlistDetector(Set.of("trusted.Caller"));

    @Test
    void allowsTrustedCaller() {
        ProvenanceGraph graph = GraphBuilder.build("X", List.of("some.Sink#method", "trusted.Caller#invoke"));
        assertFalse(detector.detect(graph).anomalous());
    }

    @Test
    void flagsUntrustedCaller() {
        ProvenanceGraph graph = GraphBuilder.build("X", List.of("some.Sink#method", "untrusted.Caller#invoke"));
        assertTrue(detector.detect(graph).anomalous());
    }

    @Test
    void failsClosedWhenCallerUnknown() {
        ProvenanceGraph graph = GraphBuilder.build("X", List.of("some.Sink#method"));
        assertTrue(detector.detect(graph).anomalous());
    }
}
