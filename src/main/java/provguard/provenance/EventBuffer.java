package provguard.provenance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory holder for captured {@link ProvenanceEvent}s.
 *
 * This is deliberately a simple, process-wide singleton: sensor Advice code is
 * bytecode-inlined into JDK sink methods (see {@code BootstrapInjector}), so it
 * cannot receive a normal dependency-injected instance. A single static
 * append-only buffer is the standard pattern for this kind of instrumentation
 * callback. {@link CopyOnWriteArrayList} is used because writes (sink hits) are
 * comparatively rare relative to reads (detection/demo code scanning events),
 * and it gives us safe concurrent iteration without external locking.
 *
 * This is a placeholder for the real event pipeline (NIO-backed TraceWriter,
 * bounded queue feeding an async detector) described in the full architecture;
 * for the hour-1 slice it exists to make the sensor's output observable and
 * testable.
 */
public final class EventBuffer {

    public static final EventBuffer INSTANCE = new EventBuffer();

    private final CopyOnWriteArrayList<ProvenanceEvent> events = new CopyOnWriteArrayList<>();

    private EventBuffer() {
    }

    public void record(ProvenanceEvent event) {
        events.add(event);
    }

    public List<ProvenanceEvent> getAll() {
        return List.copyOf(events);
    }

    public int size() {
        return events.size();
    }

    public void clear() {
        events.clear();
    }
}
