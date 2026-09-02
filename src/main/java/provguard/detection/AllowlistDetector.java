package provguard.detection;

import provguard.graph.ProvenanceGraph;

import java.util.Set;

/**
 * A signature/allowlist baseline detector - the DeseriGuard/Cristalli-style
 * trusted-path approach explicitly required as a comparison baseline in the
 * ProvGuard research plan. This is NOT a learned model; it exists precisely
 * so a future ML-based detector has something concrete to be measured
 * against (does it generalize to callers this allowlist has never seen,
 * without needing them added by hand?).
 *
 * Fails closed: if the sink's caller is unknown (stack too shallow to
 * capture) or simply not on the list, the call is flagged as anomalous.
 */
public final class AllowlistDetector implements AnomalyDetector {

    private final Set<String> trustedCallers;

    public AllowlistDetector(Set<String> trustedCallers) {
        this.trustedCallers = Set.copyOf(trustedCallers);
    }

    @Override
    public DetectionResult detect(ProvenanceGraph graph) {
        String caller = graph.callerClassName();
        if (caller == null) {
            return DetectionResult.anomaly("no caller information available (failing closed)");
        }
        if (trustedCallers.contains(caller)) {
            return DetectionResult.allow("caller " + caller + " is on the trusted allowlist");
        }
        return DetectionResult.anomaly("caller " + caller + " is NOT on the trusted allowlist");
    }
}
