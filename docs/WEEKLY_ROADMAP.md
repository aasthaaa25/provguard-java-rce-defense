# Weekly Roadmap

Planned schedule vs. actuals. Weeks not yet reached are marked **planned**, not done —
per project policy, nothing here is backfilled as complete unless it actually happened.

## Week 1 (partial — built in a single compressed session, not a full week)

**Planned:** repo skeleton, Maven build, `provguard-agent` premain that attaches and
logs, `study/01-basics` + `study/21-java-agents`.

**Actually done:**
- Maven project scaffolded and building (`pom.xml`, single module for now)
- `AgentBootstrap` with real `premain`/`agentmain`
- First real sink hooked: `ProcessBuilder#start()` via ByteBuddy `Advice`
- `StackWalker`-based provenance capture (`StackWalkerCollector`, `ProvenanceEvent`, `EventBuffer`)
- Verified end-to-end: real `-javaagent` run captures a real event with correct call stack
- `ProcessExecutionSensorTest` — passes, dynamically attaches and proves interception
- Three real classloader bugs hit and fixed (see `docs/DESIGN_DECISIONS.md`)
- Environment issues resolved and documented (`docs/TROUBLESHOOTING.md`): Avast TLS
  interception blocking Maven Central, Maven not installed, only a non-LTS JDK 26 present

**Not done from the Week 1-2 plan:** `study/` directory (no learning modules written
yet), multi-module Maven split (deferred — single module was the pragmatic choice for
this slice, see `docs/DESIGN_DECISIONS.md`), CI workflow.

## Week 2 (same compressed-session continuation)

**Planned (per the original schedule, this was slated for weeks 5-8+):** provenance
graph, additional sinks, detection baselines, enforcement.

**Actually done, ahead of the original schedule:**
- `provguard.graph`: `GraphNode`/`GraphEdge`/`ProvenanceGraph`/`GraphBuilder` — real,
  tested (`GraphBuilderTest`)
- `provguard.detection`: `AnomalyDetector` interface + `AllowlistDetector` (the real
  DeseriGuard-style baseline the research plan requires) — tested (`AllowlistDetectorTest`)
- `provguard.enforcement`: `Decision`/`PolicyEngine`/`EnforcementEngine`/`SinkBlockedException`/`Policy`
  — real ALLOW/LOG/BLOCK, BLOCK genuinely prevents sink execution — tested
  (`EnforcementEngineTest`, `PolicyEngineTest`)
- Two more sinks added and wired into the full pipeline: `ObjectInputStream.resolveClass()`
  (deserialization) and `InitialContext.lookup(String)` (JNDI — implemented but NOT
  working, see below)
- 14 `study/` Core Java modules, all run-verified (see `study/README.md`)
- Four more real bugs found and fixed (chained `AgentBuilder` rules silently dropping
  earlier sinks; the "caller is itself" graph bug for self-recursive sinks; a
  cross-classloader exception-identity bug in tests) — full writeups in
  `docs/DESIGN_DECISIONS.md` §4D-4F
- `DemoMain` rewritten to demonstrate all four ALLOW/BLOCK combinations live

**Not done / honestly incomplete:**
- JNDI sensor does not actually intercept calls at runtime despite clean weaving —
  documented as an open bug (`docs/DESIGN_DECISIONS.md` §6), test `@Disabled` not deleted
- ScriptEngine sensor not attempted (no ScriptEngine implementation on this JDK)
- No learned/ML detector — `AllowlistDetector` is the real baseline, not a placeholder,
  but it is not a model
- No dataset, no evaluation framework, no benchmark numbers

## Weeks 3+ — planned, not started

- Fix the JNDI sensor (root-cause the non-interception bug)
- `dataset/` pipeline (benign trace collector + malicious trace generation)
- Python baselines (One-Class SVM, autoencoder) + ONNX export + Java-side inference
- Concurrency hardening (virtual threads, bounded event buffer)
- Evaluation framework, robustness/evasion study
- GNN stretch goal

See the original ProvGuard planning document (shared at project start) for the full
month-by-month research framing this weekly plan is derived from.
