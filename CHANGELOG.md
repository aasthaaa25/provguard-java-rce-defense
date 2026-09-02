# Changelog

All notable changes to this project, in the order they actually happened. This is not a
promised roadmap — see `docs/WEEKLY_ROADMAP.md` for planned vs. actual.

## [Unreleased]

### Added
- Java agent (`premain`/`agentmain`) with ByteBuddy-based sink hooking
- Sensors for `ProcessBuilder#start()` (process execution) and
  `ObjectInputStream#resolveClass()` (deserialization, CWE-502), both verified
  end-to-end with real, run tests
- `provguard.provenance`: `StackWalker`-based capture, `ProvenanceEvent`, `EventBuffer`
- `provguard.graph`: `ProvenanceGraph`/`GraphBuilder` with correct caller identification
  (including for sinks that call themselves internally)
- `provguard.detection`: `AllowlistDetector`, a real signature/allowlist baseline
- `provguard.enforcement`: `PolicyEngine`/`EnforcementEngine` with genuine ALLOW/LOG/BLOCK
  — BLOCK mode actually prevents the sink's real body from executing
- `DemoMain`: live demonstration of all four allow/block combinations
- 14 self-contained, run-verified Core Java concept demos under `study/`
- Full documentation set under `docs/` (architecture, design decisions, roadmap,
  troubleshooting, core Java coverage matrix)

### Known issues
- `InitialContext#lookup(String)` (JNDI sink) is implemented but does not actually
  intercept calls at runtime — root cause not found yet; test is `@Disabled` with a
  documented reason, not deleted or silently passing (`docs/DESIGN_DECISIONS.md` §6)
- Built and tested against JDK 26 (non-LTS); the project should be re-verified against
  JDK 21 LTS before being relied on beyond a demo (`docs/DESIGN_DECISIONS.md` §2)

### Not implemented
- Script evaluation sink (no `ScriptEngine` implementation available on this JDK)
- Any learned/ML detector, dataset pipeline, or evaluation framework
