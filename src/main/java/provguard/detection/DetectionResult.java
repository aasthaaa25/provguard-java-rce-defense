package provguard.detection;

import java.time.Instant;

/**
 * The outcome of running an AnomalyDetector against one ProvenanceGraph.
 * Deliberately minimal (boolean + reason) to match what AllowlistDetector can
 * actually justify; a learned model's DetectionResult would add a numeric
 * score/threshold/model-version - not added here since no learned model
 * exists yet (see docs/ARCHITECTURE.md).
 */
public record DetectionResult(boolean anomalous, String reason, Instant timestamp) {

    public static DetectionResult allow(String reason) {
        return new DetectionResult(false, reason, Instant.now());
    }

    public static DetectionResult anomaly(String reason) {
        return new DetectionResult(true, reason, Instant.now());
    }
}
