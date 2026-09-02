# Week 2 — Test Results

```
mvn clean test
```
Result at end of week 2: `Tests run: 20, Failures: 0, Errors: 0, Skipped: 1` across:
`AllowlistDetectorTest` (3), `OneClassDistanceDetectorTest` (2), `EnforcementEngineTest` (3),
`PolicyEngineTest` (3), `GraphBuilderTest` (3), `AdviceDiagnosticTest` (1),
`DeserializationSensorTest` (1), `JndiSensorTest` (1, skipped — documented reason),
`ProcessExecutionSensorTest` (3).

Also manually re-verified via real `-javaagent` run (`DemoMain`): all four
allow/block combinations (process execution × 2, deserialization × 2) behave exactly as
expected, with real console output showing blocks happening inside the sink method
itself before its real body executes.

Both local fixtures (`fixtures/vulnerable-command-execution`,
`fixtures/vulnerable-deserialization`) were run standalone with the agent attached and
genuinely blocked — real captured output in `fixtures/README.md`.
