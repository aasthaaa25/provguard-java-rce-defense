package provguard.enforcement;

import provguard.detection.AllowlistDetector;
import provguard.detection.AnomalyDetector;

import java.util.Set;

/**
 * Process-wide policy configuration. Must be a static holder, not an
 * injected instance: it's read from Advice-inlined sensor code woven into
 * bootstrap-loaded JDK classes, which can't receive constructor-injected
 * dependencies (see provguard.provenance.EventBuffer's Javadoc, and
 * docs/DESIGN_DECISIONS.md, for the same reasoning applied there).
 *
 * The trusted-caller list here is intentionally small and explicit: it lists
 * exactly the classes in THIS repository that are expected to call a
 * dangerous sink directly (the demo and the sensor tests). Anything else -
 * including a real attacker's gadget chain - is, correctly, not on it.
 */
public final class Policy {

    public static volatile AnomalyDetector detector = new AllowlistDetector(Set.of(
            "provguard.cli.DemoMain",
            "provguard.sensors.ProcessExecutionSensorTest",
            "provguard.sensors.DeserializationSensorTest",
            "provguard.sensors.JndiSensorTest",
            "provguard.detection.OneClassDistanceDetectorTest",
            "DeserializationOverheadBenchmark",
            "DatasetCollector"
    ));

    public static volatile boolean blockingEnabled = true;

    /**
     * Off by default so existing tests/demos don't silently start writing
     * trace files. See provguard.runtime.TraceWriter and TraceWriterTest for
     * where this is actually exercised.
     */
    public static volatile boolean tracingEnabled = false;

    private Policy() {
    }
}
