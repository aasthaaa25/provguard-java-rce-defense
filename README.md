# ProvGuard

A Java Agent + provenance capture pipeline for runtime RCE defense — the flagship
project described in the ProvGuard research document. This repository is at a very
early, honestly-scoped stage: **one real sink sensor, end-to-end, tested and working.**
It is not yet the full architecture (ML detection, provenance graph, enforcement,
multiple sink types) described in `docs/ARCHITECTURE.md` — see "Current status" below
for exactly what exists today versus what's planned.

## What actually works right now

A Java agent (`java.lang.instrument`, ByteBuddy) that:
1. Attaches to a JVM (`-javaagent:provguard-agent.jar`, or dynamically via `ByteBuddyAgent.install()` in tests)
2. Weaves an `Advice` hook into the real JDK method `java.lang.ProcessBuilder#start()`
   — one of the four dangerous-sink categories in the ProvGuard threat model (process execution)
3. On every invocation, captures the call stack via `StackWalker` and records a structured
   `ProvenanceEvent` (sink type, timestamp, thread, call stack) into an in-memory buffer
4. Is proven by a real JUnit test that dynamically attaches, triggers a real `ProcessBuilder.start()`
   call, and asserts the event was captured with the correct call stack — see
   `src/test/java/provguard/sensors/ProcessExecutionSensorTest.java`

Run it yourself:
```
mvn clean test      # runs the real interception test
mvn clean package   # builds target/provguard-agent.jar
java -Dnet.bytebuddy.experimental=true -javaagent:target/provguard-agent.jar -cp target/provguard-agent.jar provguard.cli.DemoMain
```

## Current status (honest — no fabricated completion)

| Area | Status |
|---|---|
| Java agent (premain/agentmain) | ✅ Implemented, tested |
| ProcessBuilder.start() sensor + StackWalker provenance | ✅ Implemented, tested, verified via real `-javaagent` run |
| Deserialization / JNDI / ScriptEngine sensors | ❌ Not started |
| Provenance graph, feature extraction | ❌ Not started |
| ML detection (ONNX/DJL) | ❌ Not started |
| Enforcement (ALLOW/LOG/BLOCK) | ❌ Not started |
| Dataset, evaluation framework | ❌ Not started |
| `study/` Core Java learning modules | ❌ Not started |

This is genuinely a **first slice**, built to prove the hardest technical risk first:
that ProvGuard's core mechanism — weaving into a real JDK bootstrap-loaded class and
calling back into agent code — actually works on this machine's JDK. See
`docs/DESIGN_DECISIONS.md` for two real classloader bugs hit and fixed while building
this, and `docs/TROUBLESHOOTING.md` for environment-specific issues (TLS interception,
JDK version) documented for whoever continues this project.

## Requirements

- JDK: built and tested against **JDK 26** (what was available on this machine) — see
  `docs/DESIGN_DECISIONS.md` for why the project should migrate to a JDK 21 LTS build
  before this goes further, and what's untested as a result.
- Maven 3.9.9 (not on PATH by default on this machine — see `docs/TROUBLESHOOTING.md`)

## Project layout

```
src/main/java/provguard/
    agent/        AgentBootstrap (premain/agentmain), InstrumentationManager, BootstrapInjector
    sensors/      ProcessExecutionAdvice (the one live sink hook)
    provenance/   SinkType, ProvenanceEvent, EventBuffer, StackWalkerCollector
    cli/          DemoMain
src/test/java/provguard/sensors/   ProcessExecutionSensorTest, AdviceDiagnosticTest
docs/                              architecture, design decisions, roadmap, troubleshooting
```

## Documentation

- `docs/ARCHITECTURE.md` — full intended architecture (most of it not yet built)
- `docs/DESIGN_DECISIONS.md` — real decisions and bugs from this session, with reasoning
- `docs/WEEKLY_ROADMAP.md` — planned weeks vs. what's actually done
- `docs/TROUBLESHOOTING.md` — environment issues and their fixes
- `docs/CORE_JAVA_COVERAGE.md` — concept coverage matrix (small right now, honestly)

## License / ethics

Defensive security research project. All experimentation is against local code only —
no external targets, no destructive payloads.
