package provguard.graph;

/**
 * A minimal numeric summary of a ProvenanceGraph's shape, suitable as input
 * to a simple statistical (non-learned-embedding) anomaly detector. This is
 * intentionally small: depth and edge count only, no caller identity, no
 * structural embedding. See provguard.detection.OneClassDistanceDetector's
 * Javadoc and docs/ML_PIPELINE.md for why that's a real, honest limitation
 * rather than an oversight.
 */
public record FeatureVector(int depth, int edgeCount) {

    /** Simple Euclidean distance between two feature vectors. */
    public double distanceTo(FeatureVector other) {
        double dDepth = depth - other.depth;
        double dEdges = edgeCount - other.edgeCount;
        return Math.sqrt(dDepth * dDepth + dEdges * dEdges);
    }
}
