import java.util.*;

/**
 * TOPIC: List/Set/Map/Deque/PriorityQueue, equals()/hashCode(), Comparable,
 * Comparator, Collections utilities.
 *
 * WHY IT MATTERS: ProvGuard's ProvenanceGraph deduplicates repeated call
 * paths using exactly this kind of equals/hashCode-based Set membership.
 */
public class CollectionsDemo {

    // A value type with a correct equals/hashCode contract, so it can be
    // safely used as a Set element or Map key (the same requirement
    // provguard.graph.GraphNode/GraphEdge will have).
    static final class CallFrame {
        final String className;
        final String methodName;

        CallFrame(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CallFrame other)) return false;
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

    public static void main(String[] args) {
        List<CallFrame> stack = new ArrayList<>(List.of(
                new CallFrame("DemoMain", "main"),
                new CallFrame("ProcessBuilder", "start"),
                new CallFrame("ProcessBuilder", "start") // duplicate on purpose
        ));

        // Set dedup relies entirely on equals/hashCode being correct.
        Set<CallFrame> distinctFrames = new HashSet<>(stack);
        System.out.println("distinct frames: " + distinctFrames.size());

        // Map: count how many times each frame appears.
        Map<CallFrame, Integer> counts = new LinkedHashMap<>();
        for (CallFrame f : stack) {
            counts.merge(f, 1, Integer::sum);
        }
        System.out.println("counts: " + counts);

        // Deque used as a stack (push/pop) - natural fit for a call stack.
        Deque<CallFrame> callStack = new ArrayDeque<>();
        for (CallFrame f : stack) {
            callStack.push(f);
        }
        System.out.println("top of stack: " + callStack.peek());

        // PriorityQueue with a Comparator: order frames alphabetically by class.
        PriorityQueue<CallFrame> byClass = new PriorityQueue<>(Comparator.comparing(f -> f.className));
        byClass.addAll(stack);
        System.out.println("first alphabetically: " + byClass.peek());

        // Collections utility: unmodifiable view, matches EventBuffer.getAll()'s style.
        List<CallFrame> readOnly = Collections.unmodifiableList(stack);
        System.out.println("read-only size: " + readOnly.size());
    }
}
