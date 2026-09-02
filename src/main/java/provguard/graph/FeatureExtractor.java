package provguard.graph;

/** Turns a ProvenanceGraph into a FeatureVector. See FeatureVector's Javadoc for scope. */
public final class FeatureExtractor {

    private FeatureExtractor() {
    }

    public static FeatureVector extract(ProvenanceGraph graph) {
        return new FeatureVector(graph.depth(), graph.edges().size());
    }
}
