package provguard.runtime;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * A custom Java Flight Recorder event, emitted for every sink hit ProvGuard
 * evaluates. Complements (does not replace) StackWalker-based provenance
 * capture:
 *
 * - StackWalkerCollector gives ProvGuard its OWN structured call-path data,
 *   captured synchronously and used for detection/enforcement right there in
 *   SensorPipeline - it has to be synchronous because BLOCK mode depends on
 *   it.
 * - JFR gives an INDEPENDENT, standard, tooling-friendly timeline of sink
 *   hits that any JFR-aware tool (jfr print, JDK Mission Control, async-profiler,
 *   etc.) can consume without needing to know anything about ProvGuard's own
 *   internals - useful for correlating sink hits against JFR's OTHER built-in
 *   events (GC pauses, thread activity, CPU sampling) to actually explain
 *   overhead numbers like the ones in evaluation/results/, rather than only
 *   guessing at where time goes.
 *
 * {@code @StackTrace(false)}: JFR's own automatic stack-trace capture is
 * disabled deliberately - StackWalkerCollector already captures the call path
 * we care about, and JFR's default full-stack-trace capture would duplicate
 * that cost on every single sink hit for no additional benefit.
 */
@Name("ProvGuard.SinkHit")
@Category({"ProvGuard"})
@Label("Sink Hit")
@Description("A dangerous JDK sink was invoked and evaluated by ProvGuard")
@StackTrace(false)
public class SinkHitJfrEvent extends Event {

    @Label("Sink Type")
    public String sinkType;

    @Label("Caller")
    public String caller;

    @Label("Decision")
    public String decision;
}
