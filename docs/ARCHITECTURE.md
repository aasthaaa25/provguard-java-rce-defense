# ProvGuard — Architecture

## Intended full architecture (target — most not yet built)

```
Target Application
        |
        v
provguard-agent      (premain via java.lang.instrument; agentmain planned)
        |
        v
provguard-sensors    (ByteBuddy Advice hooks on dangerous JDK sinks)
        |  hooks: ObjectInputStream.resolveClass/readObject [NOT BUILT],
        |         Context.lookup [NOT BUILT], ProcessBuilder.start [BUILT],
        |         ScriptEngine.eval [NOT BUILT]
        v
provguard-provenance (StackWalker capture -> ProvenanceEvent record)         [BUILT for the one sensor]
        v
provguard-graph       (ProvenanceGraph / FeatureExtractor)                   [NOT BUILT]
        v
provguard-detection    (AnomalyDetector -> ONNX/DJL -> DetectionResult)      [NOT BUILT]
        v
provguard-enforcement  (PolicyEngine -> ALLOW / LOG / BLOCK)                 [NOT BUILT]
        v
Application continues, or the dangerous operation is denied
```

## What exists today

Only the vertical slice: `provguard-agent` -> `provguard-sensors` (one sensor:
`ProcessExecutionAdvice` on `ProcessBuilder#start()`) -> `provguard-provenance`
(`StackWalkerCollector` + `ProvenanceEvent` + `EventBuffer`). Everything from
`provguard-graph` onward is not implemented — there is no detection, no enforcement, no
ML. `EventBuffer` is a simple in-memory `CopyOnWriteArrayList`, not the planned
NIO-backed `TraceWriter`/bounded-queue pipeline.

## Package layout (current, single Maven module)

```
provguard.agent        AgentBootstrap (premain/agentmain entrypoints)
                        InstrumentationManager (wires ByteBuddy AgentBuilder + the sensor)
                        BootstrapInjector (makes provguard.* classes visible to the
                                            bootstrap classloader — see DESIGN_DECISIONS.md)
provguard.sensors       ProcessExecutionAdvice (the one live Advice hook)
provguard.provenance    SinkType, ProvenanceEvent, EventBuffer, StackWalkerCollector
provguard.cli           DemoMain
```

The full multi-module split described in the original brief
(`provguard-agent`/`provguard-sensors`/`provguard-provenance`/`provguard-graph`/
`provguard-detection`/`provguard-enforcement`/`provguard-runtime`/`provguard-config`/
`provguard-cli`/`provguard-common`) is deferred until there's enough real code per
concern to justify separate Maven modules — right now it would just be empty
directories.

## Why ProcessBuilder.start() was the first sink implemented

It's a real, unambiguous RCE-relevant sink (command execution), requires no external
vulnerable-library setup to demonstrate (unlike a deserialization gadget chain, which
needs a real gadget library on the classpath), and it's loaded by the bootstrap
classloader — meaning it forces confronting the hardest real technical risk (calling
back from bootstrap-woven bytecode into agent code) immediately, rather than deferring
it. See `docs/DESIGN_DECISIONS.md` for the three classloader bugs this surfaced and
fixed.

## Threat model note (honest, per project policy)

This slice detects/observes exactly one call pattern: any code path that calls
`new ProcessBuilder(...).start()`. It does not distinguish benign from malicious
invocations (no ML yet), does not block anything yet (no enforcement layer yet), and
covers only 1 of the 4 sink categories in the ProvGuard threat model. See
`README.md` "Current status" for the full honest breakdown.
