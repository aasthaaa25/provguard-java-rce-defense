# ProvGuard

A Java Agent + provenance capture + detection + enforcement pipeline for runtime RCE
defense — the flagship project described in the ProvGuard research document. This
repository has a genuinely working end-to-end prototype for **two of the four** sink
categories in the threat model, with real detection (allowlist baseline) and real
enforcement (it can actually block a call, not just log it). It is not yet the full
research architecture (ML detection, full provenance graph feature learning, dataset,
evaluation framework) described in `docs/ARCHITECTURE.md` — see "Current status" below
for exactly what exists today versus what's planned.

## What actually works right now

A Java agent (`java.lang.instrument`, ByteBuddy) that, for **process execution**
(`ProcessBuilder#start()`) and **deserialization** (`ObjectInputStream#resolveClass()`):

1. Attaches to a JVM (`-javaagent:provguard-agent.jar`, or dynamically via `ByteBuddyAgent.install()` in tests)
2. Weaves an `Advice` hook into the real JDK sink method
3. Captures the call stack via `StackWalker` into a structured `ProvenanceEvent`
4. Builds a `ProvenanceGraph` from that call stack (nodes/edges, caller identification)
5. Runs a real `AllowlistDetector` (a genuine signature/allowlist baseline — not a
   learned model, but a real, working, testable detector) against the graph
6. Turns the detection result into an `ALLOW` / `LOG` / `BLOCK` decision via `PolicyEngine`
7. **Actually enforces it**: in `BLOCK` mode, `EnforcementEngine` throws before the sink's
   real body ever runs — this is a genuine prevention mechanism, verified by tests where
   the underlying `ProcessBuilder`/`ObjectInputStream` call demonstrably never executes.

Proven by real, run-verified tests (17 passing, 1 honestly disabled — see below), not
mocks: `ProcessExecutionSensorTest`, `DeserializationSensorTest`, `GraphBuilderTest`,
`AllowlistDetectorTest`, `PolicyEngineTest`, `EnforcementEngineTest`.

Run it yourself:
```
mvn clean test      # runs the full real test suite
mvn clean package   # builds target/provguard-agent.jar
java -Dnet.bytebuddy.experimental=true -javaagent:target/provguard-agent.jar -cp target/provguard-agent.jar provguard.cli.DemoMain
```
`DemoMain` demonstrates all four combinations live: an allowed process execution, a
blocked one (untrusted caller), an allowed deserialization, and a blocked one — with
real console output showing the block actually happening.

## Current status (honest — no fabricated completion)

| Area | Status |
|---|---|
| Java agent (premain/agentmain) | ✅ Implemented, tested |
| ProcessBuilder.start() sensor | ✅ Implemented, tested, verified via real `-javaagent` run |
| ObjectInputStream.resolveClass() sensor | ✅ Implemented, tested, verified via real `-javaagent` run |
| InitialContext.lookup(String) sensor | ⚠️ Implemented, but does NOT actually intercept calls — a real, unresolved bug (test disabled, not deleted) — see `docs/DESIGN_DECISIONS.md` §4F |
| ScriptEngine.eval() sensor | ❌ Not implemented — this JDK has no bundled ScriptEngine (Nashorn removed) to hook/test against |
| Provenance graph (nodes/edges/caller extraction) | ✅ Implemented, tested |
| Detection: allowlist baseline | ✅ Implemented, tested — this is the real DeseriGuard-style baseline the research plan calls for |
| Detection: learned models (SVM/autoencoder/GNN) | ❌ Not started — needs real training data and a training run, neither of which exist here |
| Enforcement (ALLOW/LOG/BLOCK) | ✅ Implemented, tested — BLOCK genuinely prevents sink execution |
| Dataset, evaluation framework | ❌ Not started |
| `study/` Core Java learning modules | ✅ 14 modules, all run-verified — see `study/README.md` |

This is a real, working prototype for its scope — not a pile of placeholder files. What
it is NOT: a learned/ML detector (the allowlist baseline is what a future model would
need to beat), complete sink coverage (2 of 4, honestly), or a measured/evaluated system
(no dataset, no benchmark numbers exist — anything like that would be fabricated if
claimed here).

See `docs/DESIGN_DECISIONS.md` for **six** real bugs hit and fixed while building this
(five resolved, one — JNDI — documented as open), and `docs/TROUBLESHOOTING.md` for
environment-specific issues (TLS interception, JDK version, Maven not installed).

## Requirements

- JDK: built and tested against **JDK 26** (what was available on this machine) — see
  `docs/DESIGN_DECISIONS.md` for why the project should migrate to a JDK 21 LTS build
  before this goes further, and what's untested as a result.
- Maven 3.9.9 (not on PATH by default on this machine — see `docs/TROUBLESHOOTING.md`)
- Run with `-Dnet.bytebuddy.experimental=true` (needed because of the JDK 26 point above)

## Project layout

```
src/main/java/provguard/
    agent/         AgentBootstrap (premain/agentmain), InstrumentationManager, BootstrapInjector
    sensors/       ProcessExecutionAdvice, DeserializationAdvice, JndiAdvice, SensorPipeline
    provenance/    SinkType, ProvenanceEvent, EventBuffer, StackWalkerCollector
    graph/         GraphNode, GraphEdge, ProvenanceGraph, GraphBuilder
    detection/     AnomalyDetector, AllowlistDetector, DetectionResult
    enforcement/   Decision, PolicyEngine, EnforcementEngine, SinkBlockedException, Policy
    cli/           DemoMain
src/test/java/provguard/    mirrors the above, one test class per component
study/                       14 self-contained Core Java concept demos (see study/README.md)
fixtures/                    2 minimal local vulnerable programs to demo real blocking (see fixtures/README.md)
docs/                        architecture, design decisions, roadmap, troubleshooting
```

## Documentation

- `docs/ARCHITECTURE.md` — full intended architecture, current vs. planned
- `docs/THREAT_MODEL.md` — what's actually protected against, and what explicitly isn't
- `docs/SECURITY.md` — reporting, ethics of testing, known security-relevant limitations
- `docs/DESIGN_DECISIONS.md` — every real decision and bug from building this, with reasoning
- `docs/WEEKLY_ROADMAP.md` — planned weeks vs. what's actually done
- `docs/TROUBLESHOOTING.md` — environment issues and their fixes
- `docs/CORE_JAVA_COVERAGE.md` — concept coverage matrix
- `docs/STUDY_REPOSITORIES.md` — external repos worth reading, and why
- `docs/ML_PIPELINE.md`, `docs/DATASET.md`, `docs/EVALUATION.md` — plans only; all
  explicitly marked not-yet-started rather than left unmentioned
- `CHANGELOG.md`, `CONTRIBUTING.md`, `LICENSE` — standard project files

## License / ethics

Defensive security research project. All experimentation is against local code only —
no external targets, no destructive payloads.
