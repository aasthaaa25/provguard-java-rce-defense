package provguard.graph;

import java.util.Objects;

/**
 * One call-stack frame in a provenance graph. Deliberately a plain class (not
 * a record) with a hand-written equals/hashCode so it's usable as a Set
 * element / Map key for deduplication - what it needs to be, not more.
 */
public final class GraphNode {

    private final String className;
    private final String methodName;

    public GraphNode(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public String className() {
        return className;
    }

    public String methodName() {
        return methodName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GraphNode other)) {
            return false;
        }
        return className.equals(other.className) && methodName.equals(other.methodName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName);
    }

    @Override
    public String toString() {
        return className + "#" + methodName;
    }
}
