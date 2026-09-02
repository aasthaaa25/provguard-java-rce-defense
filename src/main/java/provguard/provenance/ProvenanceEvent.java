package provguard.provenance;

import java.time.Instant;
import java.util.List;

/**
 * An immutable record of a single dangerous-sink invocation, captured at the
 * moment the sink method was entered. Deliberately does NOT capture method
 * arguments or any payload data — per project policy we never log secrets,
 * credentials, or arbitrary user input, only structural call-path metadata.
 */
public record ProvenanceEvent(
        SinkType sinkType,
        Instant timestamp,
        String threadName,
        List<String> callStack
) {
    public String summary() {
        return "[%s] thread=%s stack=%s".formatted(sinkType, threadName, callStack);
    }
}
