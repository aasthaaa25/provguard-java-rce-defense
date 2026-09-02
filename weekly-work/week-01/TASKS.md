# Week 1 — Tasks

- [x] Install Maven (not present on this machine — downloaded manually, see `docs/TROUBLESHOOTING.md`)
- [x] Resolve Maven Central access (Avast TLS interception was blocking it)
- [x] Scaffold single-module Maven project
- [x] `AgentBootstrap`: real `premain`/`agentmain`
- [x] `InstrumentationManager` + `BootstrapInjector`: weave `ProcessBuilder#start()`
- [x] `StackWalkerCollector`, `ProvenanceEvent`, `EventBuffer`: real provenance capture
- [x] `ProcessExecutionSensorTest`: proves real interception (not a mock)
- [x] Verify via real `-javaagent` run (`DemoMain`), not just the test
- [x] `study/` Core Java modules (14, all run-verified)
- [ ] Multi-module Maven split — deferred, documented decision (`docs/DESIGN_DECISIONS.md` §1)
