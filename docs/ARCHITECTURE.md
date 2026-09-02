# ProvGuard — Architecture

## Intended full architecture (target — ML/graph-features/dataset/evaluation not yet built)

```
Target Application
        |
        v
provguard.agent      (premain/agentmain via java.lang.instrument)
        |
        v
provguard.sensors    (ByteBuddy Advice hooks on dangerous JDK sinks)
        |  hooks: ObjectInputStream.resolveClass          [BUILT]
        |         InitialContext.lookup(String)             [BUILT, but not functioning — see docs/DESIGN_DECISIONS.md §6]
        |         ProcessBuilder.start                     [BUILT]
        |         ScriptEngine.eval                        [NOT BUILT — no ScriptEngine impl on this JDK to test against]
        v
provguard.provenance  (StackWalker capture -> ProvenanceEvent record)        [BUILT]
        v
provguard.graph        (ProvenanceGraph: nodes/edges/caller-extraction)      [BUILT — simple, not yet ML-feature-rich]
        v
provguard.detection     (AnomalyDetector -> AllowlistDetector)               [BUILT — baseline only, no learned model]
        v
provguard.enforcement    (PolicyEngine -> ALLOW / LOG / BLOCK)               [BUILT — BLOCK genuinely prevents execution]
        v
Application continues, or SinkBlockedException denies the operation
```

## What exists today (all real, tested, run-verified)

The full pipeline works end-to-end for **two** sink types (process execution,
deserialization): `provguard.agent` → `provguard.sensors` → `provguard.provenance` →
`provguard.graph` → `provguard.detection` (`AllowlistDetector`, a signature/allowlist
baseline — not ML) → `provguard.enforcement` (real ALLOW/LOG/BLOCK, where BLOCK actually
throws before the sink's real body executes). See `README.md`'s "Current status" table
and `docs/DESIGN_DECISIONS.md` for the honest, itemized breakdown including one sink
(JNDI) whose weaving reports success but doesn't actually intercept calls — a real,
documented, unresolved bug, not silently ignored.

Not built: any learned/ML detector (`AllowlistDetector` is a real, working baseline — the
thing a future model needs to beat, not a placeholder), the dataset pipeline, the
evaluation framework, feature extraction beyond what `AllowlistDetector` needs.

## Package layout (current, single Maven module)

```
provguard.agent        AgentBootstrap (premain/agentmain entrypoints)
                        InstrumentationManager (installs one AgentBuilder per sink)
                        BootstrapInjector (makes provguard.* classes visible to the
                                            bootstrap classloader — see DESIGN_DECISIONS.md)
provguard.sensors       ProcessExecutionAdvice, DeserializationAdvice, JndiAdvice
                        SensorPipeline (shared capture -> graph -> detect -> enforce)
provguard.provenance    SinkType, ProvenanceEvent, EventBuffer, StackWalkerCollector
provguard.graph         GraphNode, GraphEdge, ProvenanceGraph, GraphBuilder
provguard.detection     AnomalyDetector, AllowlistDetector, DetectionResult
provguard.enforcement   Decision, PolicyEngine, EnforcementEngine, SinkBlockedException, Policy
provguard.cli           DemoMain
```

The full multi-module split described in the original brief
(separate `provguard-agent`/`provguard-sensors`/etc. Maven modules) is still deferred:
one module with clean package boundaries is proportionate to the current code volume;
splitting into real Maven modules is worth doing once there's enough code per concern
(e.g. once ML/detection genuinely grows) to justify the build overhead.

## Why these three sinks, in this order

`ProcessBuilder.start()` first: unambiguous RCE-relevant sink, no external vulnerable
library needed to demonstrate, and bootstrap-classloader-loaded — forces confronting the
hardest classloader risk immediately (see `docs/DESIGN_DECISIONS.md` §4).
`ObjectInputStream.resolveClass()` second: the canonical CWE-502 deserialization sink,
reused the same proven instrumentation pattern, and surfaced two more real bugs (the
`AgentBuilder` chaining issue and the "caller is itself" graph bug) that a second sink was
exactly what was needed to expose. `InitialContext.lookup(String)` third: attempted for
completeness (Log4Shell-style JNDI injection), implemented, but doesn't actually work —
documented as open follow-up rather than hidden. `ScriptEngine.eval()` was not attempted:
this JDK ships no bundled scripting engine (Nashorn was removed after JDK 14), so there is
nothing genuine to hook and test without adding an external dependency.

## Why AllowlistDetector, not a stub

`AllowlistDetector` is a real signature/allowlist baseline (the DeseriGuard/Cristalli-style
approach explicitly called out as a required comparison baseline in the ProvGuard research
plan), fully wired into real enforcement. It fails closed (unknown caller = anomalous) and
is genuinely enforced (BLOCK mode really prevents the sink from running — verified by
tests where the underlying JDK call demonstrably never executes). This is more valuable at
this stage than an `AnomalyDetector` stub waiting for a model that doesn't exist: it makes
the enforcement layer real and testable now, and gives any future learned model a concrete
target to beat.

## Threat model note (honest, per project policy)

Detects/enforces exactly two call patterns today: `ProcessBuilder.start()` and
`ObjectInputStream.resolveClass()`, judged purely by whether the immediate external caller
class is on a small, explicit allowlist (no ML, no structural/graph-shape analysis beyond
identifying that caller). It does not detect novel gadget chains by *structure* — only by
*origin*. A sufficiently determined attacker who can make a malicious call originate from
an allowlisted class (e.g. by compromising code that already runs there) would not be
caught by this baseline; that is exactly the kind of gap a future learned model over graph
structure is meant to close. See `README.md` "Current status" for the full honest
breakdown of what is and isn't built.
