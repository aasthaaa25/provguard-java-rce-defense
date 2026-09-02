package provguard.graph;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GraphBuilderTest {

    @Test
    void skipsOwnInfrastructureFramesAndBuildsNodesInOrder() {
        List<String> rawStack = List.of(
                "provguard.provenance.StackWalkerCollector#captureCallStack",
                "provguard.sensors.SensorPipeline#captureAndEnforce",
                "java.lang.ProcessBuilder#start",
                "provguard.cli.DemoMain#main"
        );

        ProvenanceGraph graph = GraphBuilder.build("PROCESS_EXECUTION", rawStack);

        assertEquals(2, graph.depth(), "Both infrastructure frames should be skipped");
        assertEquals("java.lang.ProcessBuilder", graph.nodes().get(0).className());
        assertEquals("provguard.cli.DemoMain", graph.callerClassName());
        assertEquals(1, graph.edges().size());
    }

    @Test
    void returnsNullCallerWhenStackTooShort() {
        ProvenanceGraph graph = GraphBuilder.build("PROCESS_EXECUTION", List.of("java.lang.ProcessBuilder#start"));
        assertNull(graph.callerClassName());
    }

    @Test
    void edgeConnectsSinkNodeToCallerNode() {
        ProvenanceGraph graph = GraphBuilder.build("X", List.of("some.Sink#method", "some.Caller#invoke"));
        GraphEdge edge = graph.edges().iterator().next();
        assertEquals(new GraphNode("some.Sink", "method"), edge.from());
        assertEquals(new GraphNode("some.Caller", "invoke"), edge.to());
    }
}
