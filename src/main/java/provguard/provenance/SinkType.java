package provguard.provenance;

/**
 * The category of dangerous JDK sink a ProvGuard sensor is watching.
 * Only PROCESS_EXECUTION is actually wired up to a live sensor in this
 * hour-1 slice; the others are declared here so downstream code (detection,
 * enforcement, docs) has a stable vocabulary to grow into.
 */
public enum SinkType {
    PROCESS_EXECUTION,
    DESERIALIZATION,
    JNDI_LOOKUP,
    SCRIPT_EVALUATION
}
