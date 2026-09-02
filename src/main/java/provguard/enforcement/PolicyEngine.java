package provguard.enforcement;

import provguard.detection.DetectionResult;

/**
 * Turns a detector's raw DetectionResult into an enforcement Decision.
 * Deliberately separate from AnomalyDetector (detection asks "is this
 * unusual?"; policy asks "given that, what do we actually do?") and from
 * EnforcementEngine (which only knows how to carry a Decision out) - see
 * docs/ARCHITECTURE.md for why these three stay separate.
 */
public final class PolicyEngine {

    private final boolean blockingEnabled;

    public PolicyEngine(boolean blockingEnabled) {
        this.blockingEnabled = blockingEnabled;
    }

    public Decision decide(DetectionResult result) {
        if (!result.anomalous()) {
            return new Decision.Allow();
        }
        return blockingEnabled ? new Decision.Block(result.reason()) : new Decision.Log(result.reason());
    }
}
