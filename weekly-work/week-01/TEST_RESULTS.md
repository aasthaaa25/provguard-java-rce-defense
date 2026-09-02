# Week 1 — Test Results

```
mvn clean test
```
Result at end of week 1: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` —
`ProcessExecutionSensorTest` (2 tests) + `AdviceDiagnosticTest` (1 test).

Also manually verified via real `-javaagent` run (`DemoMain`): agent attaches, sensor
fires, one `ProvenanceEvent` captured with the correct call stack — see
`docs/DESIGN_DECISIONS.md` for the exact captured output.
