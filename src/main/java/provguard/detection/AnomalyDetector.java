package provguard.detection;

import provguard.graph.ProvenanceGraph;

/**
 * Strategy interface for scoring a ProvenanceGraph. AllowlistDetector is the
 * only implementation today; learned baselines (one-class SVM, autoencoder,
 * GNN) described in the ProvGuard research plan are future work and would
 * implement this same interface so callers never branch on concrete type.
 */
public interface AnomalyDetector {
    DetectionResult detect(ProvenanceGraph graph);
}
