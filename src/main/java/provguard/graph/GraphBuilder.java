package provguard.graph;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts a raw call stack (as captured by StackWalkerCollector, each frame
 * formatted "ClassName#methodName") into a structured ProvenanceGraph.
 *
 * Skips ProvGuard's OWN infrastructure frames (the capture/pipeline plumbing
 * that sits between the sink method and StackWalker) since they are an
 * implementation detail of how the stack was captured, not part of the
 * application's real call path. Getting this list right matters: an
 * incomplete skip-list previously caused callerClassName() to return the
 * sink's own class name instead of the real caller after SensorPipeline was
 * introduced as a shared helper - see docs/DESIGN_DECISIONS.md section 5.
 */
public final class GraphBuilder {

    private static final Set<String> OWN_INFRASTRUCTURE_CLASSES = Set.of(
            "provguard.provenance.StackWalkerCollector",
            "provguard.sensors.SensorPipeline"
    );

    private GraphBuilder() {
    }

    public static ProvenanceGraph build(String sinkType, List<String> rawCallStack) {
        List<GraphNode> nodes = new ArrayList<>();
        for (String frame : rawCallStack) {
            String frameClassName = frame.contains("#") ? frame.substring(0, frame.indexOf('#')) : frame;
            if (OWN_INFRASTRUCTURE_CLASSES.contains(frameClassName)) {
                continue;
            }
            int hashIndex = frame.indexOf('#');
            String className = hashIndex >= 0 ? frame.substring(0, hashIndex) : frame;
            String methodName = hashIndex >= 0 ? frame.substring(hashIndex + 1) : "";
            nodes.add(new GraphNode(className, methodName));
        }

        Set<GraphEdge> edges = new LinkedHashSet<>();
        for (int i = 0; i < nodes.size() - 1; i++) {
            edges.add(new GraphEdge(nodes.get(i), nodes.get(i + 1)));
        }

        return new ProvenanceGraph(sinkType, nodes, edges);
    }
}
