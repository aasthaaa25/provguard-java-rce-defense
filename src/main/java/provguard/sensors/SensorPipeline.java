package provguard.sensors;

import provguard.detection.DetectionResult;
import provguard.enforcement.Decision;
import provguard.enforcement.EnforcementEngine;
import provguard.enforcement.Policy;
import provguard.enforcement.PolicyEngine;
import provguard.graph.GraphBuilder;
import provguard.graph.ProvenanceGraph;
import provguard.provenance.EventBuffer;
import provguard.provenance.ProvenanceEvent;
import provguard.provenance.SinkType;
import provguard.provenance.StackWalkerCollector;
import provguard.runtime.TraceWriter;

import java.time.Instant;
import java.util.List;

/**
 * The full capture -> graph -> detect -> decide -> [trace] -> enforce
 * pipeline, shared by every sensor's Advice.OnMethodEnter method so each one
 * stays a one-liner. Like ProcessExecutionAdvice, this is called from
 * bytecode inlined into a bootstrap-loaded JDK class, so it (and everything
 * it calls) must live under the provguard.* package that BootstrapInjector
 * makes visible there - see docs/DESIGN_DECISIONS.md.
 *
 * Async tracing (if enabled) happens BEFORE enforce() deliberately: enforce()
 * throws for a BLOCK decision, and blocked calls are exactly the ones most
 * worth having a trace record of.
 */
public final class SensorPipeline {

    private SensorPipeline() {
    }

    public static void captureAndEnforce(SinkType sinkType) {
        List<String> callStack = StackWalkerCollector.captureCallStack();
        ProvenanceEvent event = new ProvenanceEvent(sinkType, Instant.now(), Thread.currentThread().getName(), callStack);
        EventBuffer.INSTANCE.record(event);

        ProvenanceGraph graph = GraphBuilder.build(sinkType.name(), callStack);
        DetectionResult result = Policy.detector.detect(graph);
        Decision decision = new PolicyEngine(Policy.blockingEnabled).decide(result);

        if (Policy.tracingEnabled) {
            TraceWriter.writeAsync(TraceWriter.defaultTraceFile(), event, decision);
        }

        EnforcementEngine.enforce(decision, sinkType + " via " + graph.callerClassName());
    }
}
