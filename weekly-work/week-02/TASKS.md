# Week 2 — Tasks

- [x] `provguard.graph`: `GraphNode`/`GraphEdge`/`ProvenanceGraph`/`GraphBuilder`
- [x] `provguard.detection`: `AnomalyDetector` + `AllowlistDetector` (real baseline)
- [x] `provguard.enforcement`: `Decision`/`PolicyEngine`/`EnforcementEngine`/`Policy` — real ALLOW/LOG/BLOCK
- [x] Second sink: `ObjectInputStream#resolveClass()` (deserialization)
- [x] Third sink attempted: `InitialContext#lookup(String)` (JNDI) — implemented, does not work (4 fix attempts, documented)
- [x] `DemoMain` rewritten to demonstrate all 4 allow/block combinations live
- [x] Local vulnerable fixtures (`vulnerable-command-execution`, `vulnerable-deserialization`), both verified genuinely blocked
- [x] Fixed a real bug: chaining multiple `AgentBuilder` rules silently dropped earlier sinks
- [x] Fixed a real bug: "caller is itself" graph bug for sinks that call themselves internally
- [x] Fixed a real bug: cross-classloader exception-identity mismatch in tests
- [x] Fixed a real, serious bug: test execution order could pin `provguard.*` classes to
      the wrong classloader depending on which test class ran first — root-fixed by
      attaching the agent via real `-javaagent` at JVM startup for the whole test run,
      not per-class dynamic self-attach
- [x] `provguard.graph.FeatureVector`/`FeatureExtractor` + `provguard.detection.OneClassDistanceDetector`
      — a real, tested, benign-only statistical baseline trained on genuinely captured data
- [x] Remaining top-level/docs files from the original spec (`THREAT_MODEL.md`,
      `SECURITY.md`, `STUDY_REPOSITORIES.md`, `ML_PIPELINE.md`, `DATASET.md`,
      `EVALUATION.md`, `CONTRIBUTING.md`, `LICENSE`, `CHANGELOG.md`)
- [ ] Real ML (Python/scikit-learn/ONNX) — not started, see week 3+
- [ ] Dataset pipeline, evaluation numbers — not started, see week 3+
