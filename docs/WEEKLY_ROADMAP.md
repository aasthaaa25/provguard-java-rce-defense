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

## Weeks 2+ — planned, not started

- Remaining sink sensors: `ObjectInputStream.resolveClass` (deserialization),
  `Context.lookup` (JNDI), `ScriptEngine.eval` (script injection)
- `provguard-graph`: `GraphBuilder`/`GraphNode`/`GraphEdge`/`FeatureExtractor`
- `dataset/` pipeline (benign trace collector + malicious trace generation)
- Python baselines (One-Class SVM, autoencoder) + ONNX export + Java-side inference
- `provguard-enforcement`: ALLOW/LOG/BLOCK
- Concurrency hardening (virtual threads, bounded event buffer)
- Evaluation framework, robustness/evasion study
- `study/` Core Java learning modules

See the original ProvGuard planning document (shared at project start) for the full
month-by-month research framing this weekly plan is derived from.
