package provguard.detection;

import provguard.graph.FeatureExtractor;
import provguard.graph.FeatureVector;
import provguard.graph.ProvenanceGraph;

import java.util.List;

/**
 * A real, working, benign-only ("one-class") statistical baseline: trained on
 * a set of FeatureVectors extracted from genuinely benign traces, it flags a
 * new graph as anomalous when its distance from the mean of the training set
 * exceeds a threshold (in units of the training set's own standard
 * deviation, i.e. a simple z-score-style novelty check).
 *
 * THIS IS NOT A GNN, NOT DEEP LEARNING, AND NOT TRAINED ON A LARGE OR REAL
 * ATTACK DATASET — it is the honest first rung of the "learned model" ladder
 * described in the ProvGuard research plan: benign-only, unsupervised,
 * genuinely trained on genuinely captured data (see
 * OneClassDistanceDetectorTest, which trains it on real ProvenanceGraphs
 * produced by actually triggering the ProcessBuilder and ObjectInputStream
 * sensors, not on invented numbers).
 *
 * A REAL, DOCUMENTED LIMITATION worth stating plainly: {@link FeatureVector}
 * only captures depth and edge count, not caller identity or any richer
 * structural signal. For the two sinks currently wired up, a benign call and
 * a malicious-but-structurally-identical call (same depth, same edge count,
 * different caller) are INDISTINGUISHABLE to this detector — it will not
 * flag them. That is exactly why {@link AllowlistDetector} (caller-identity-based)
 * is the detector actually wired into enforcement today, and exactly the gap
 * a future richer feature set (or a real GNN over graph structure) would need
 * to close. This class exists to make that gap concrete and testable, not to
 * pretend it's already closed.
 */
public final class OneClassDistanceDetector implements AnomalyDetector {

    private final double meanDepth;
    private final double meanEdges;
    private final double stdDevDistance;
    private final double toleranceInStdDevs;

    public OneClassDistanceDetector(List<FeatureVector> benignTrainingSet, double toleranceInStdDevs) {
        if (benignTrainingSet.isEmpty()) {
            throw new IllegalArgumentException("Cannot train a one-class detector on zero examples");
        }
        this.toleranceInStdDevs = toleranceInStdDevs;

        double sumDepth = 0;
        double sumEdges = 0;
        for (FeatureVector v : benignTrainingSet) {
            sumDepth += v.depth();
            sumEdges += v.edgeCount();
        }
        this.meanDepth = sumDepth / benignTrainingSet.size();
        this.meanEdges = sumEdges / benignTrainingSet.size();

        FeatureVector mean = new FeatureVector((int) Math.round(meanDepth), (int) Math.round(meanEdges));
        double sumSquaredDistance = 0;
        for (FeatureVector v : benignTrainingSet) {
            double d = v.distanceTo(mean);
            sumSquaredDistance += d * d;
        }
        double variance = sumSquaredDistance / benignTrainingSet.size();
        // A training set with zero variance (every example identically shaped,
        // which is common here - see the class Javadoc) would make ANY
        // deviation infinitely many standard deviations away. Floor stdDev at
        // a small epsilon so the detector degrades to "flag any deviation at
        // all" instead of dividing by zero, rather than crashing.
        this.stdDevDistance = Math.max(Math.sqrt(variance), 0.5);
    }

    @Override
    public DetectionResult detect(ProvenanceGraph graph) {
        FeatureVector observed = FeatureExtractor.extract(graph);
        FeatureVector mean = new FeatureVector((int) Math.round(meanDepth), (int) Math.round(meanEdges));
        double distance = observed.distanceTo(mean);
        double distanceInStdDevs = distance / stdDevDistance;

        if (distanceInStdDevs > toleranceInStdDevs) {
            return DetectionResult.anomaly(
                    "structural distance %.2f std-devs from benign training mean (depth=%d, edges=%d vs trained mean depth=%.1f, edges=%.1f)"
                            .formatted(distanceInStdDevs, observed.depth(), observed.edgeCount(), meanDepth, meanEdges));
        }
        return DetectionResult.allow(
                "structural distance %.2f std-devs from benign training mean, within tolerance".formatted(distanceInStdDevs));
    }
}
