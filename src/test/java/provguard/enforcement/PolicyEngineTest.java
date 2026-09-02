package provguard.enforcement;

import org.junit.jupiter.api.Test;
import provguard.detection.DetectionResult;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// Agent attached at JVM startup for the whole test run - see pom.xml and
// docs/DESIGN_DECISIONS.md section 4G.
class PolicyEngineTest {

    @Test
    void allowsWhenDetectionIsNotAnomalous() {
        Decision decision = new PolicyEngine(true).decide(DetectionResult.allow("fine"));
        assertInstanceOf(Decision.Allow.class, decision);
    }

    @Test
    void blocksAnomalyWhenBlockingEnabled() {
        Decision decision = new PolicyEngine(true).decide(DetectionResult.anomaly("suspicious"));
        assertInstanceOf(Decision.Block.class, decision);
    }

    @Test
    void onlyLogsAnomalyWhenBlockingDisabled() {
        Decision decision = new PolicyEngine(false).decide(DetectionResult.anomaly("suspicious"));
        assertInstanceOf(Decision.Log.class, decision);
    }
}
