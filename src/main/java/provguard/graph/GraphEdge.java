package provguard.graph;

import java.util.Objects;

/** A directed callee -> caller relationship between two frames. */
public final class GraphEdge {

    private final GraphNode from;
    private final GraphNode to;

    public GraphEdge(GraphNode from, GraphNode to) {
        this.from = from;
        this.to = to;
    }

    public GraphNode from() {
        return from;
    }

    public GraphNode to() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GraphEdge other)) {
            return false;
        }
        return from.equals(other.from) && to.equals(other.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
