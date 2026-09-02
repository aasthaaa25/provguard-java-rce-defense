package provguard.graph;

import java.util.List;
import java.util.Set;

/**
 * A structured representation of one sink invocation's call path - nodes are
 * distinct call frames, edges are caller/callee relationships. This is the
 * minimal real version of the "unified provenance abstraction" the ProvGuard
 * research plan calls for; feature extraction beyond depth/caller (e.g. for a
 * learned model) is future work, not built here.
 */
public final class ProvenanceGraph {

    private final String sinkType;
    private final List<GraphNode> nodes; // ordered: nodes.get(0) is the sink frame itself
    private final Set<GraphEdge> edges;

    public ProvenanceGraph(String sinkType, List<GraphNode> nodes, Set<GraphEdge> edges) {
        this.sinkType = sinkType;
        this.nodes = List.copyOf(nodes);
        this.edges = Set.copyOf(edges);
    }

    public String sinkType() {
        return sinkType;
    }

    public List<GraphNode> nodes() {
        return nodes;
    }

    public Set<GraphEdge> edges() {
        return edges;
    }

    /** Number of distinct frames captured (excluding ProvGuard's own capture frame). */
    public int depth() {
        return nodes.size();
    }

    /**
     * The class name of whoever really called the sink, skipping past any
     * frames that belong to the sink's own class.
     *
     * This matters for sinks like {@code ObjectInputStream.resolveClass()}
     * that are invoked internally by OTHER methods of that same class (e.g.
     * {@code readObject -> readOrdinaryObject -> resolveClass}, all within
     * {@code ObjectInputStream}): the naive "next frame up" would report the
     * sink class as its own caller, which is never useful for an allowlist
     * decision. Returns {@code null} if no frame outside the sink's own
     * class is found (or the stack was too shallow to capture at all).
     * {@code AllowlistDetector} treats {@code null} as fail-closed.
     */
    public String callerClassName() {
        if (nodes.isEmpty()) {
            return null;
        }
        String sinkClassName = nodes.get(0).className();
        for (int i = 1; i < nodes.size(); i++) {
            if (!nodes.get(i).className().equals(sinkClassName)) {
                return nodes.get(i).className();
            }
        }
        return null;
    }
}
