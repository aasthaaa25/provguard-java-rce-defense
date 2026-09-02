# Week 2 — Notes

The most valuable bug this week wasn't in production code at all — it was in the test
suite's own setup. Adding a new test class changed Maven Surefire's execution order
enough that a *pure unit test* (one that never installs the agent itself) became the
first code in the JVM to touch `provguard.graph`/`detection`/`enforcement` types,
pinning them to the application classloader before any sensor test's `@BeforeAll` ever
ran bootstrap injection. The fix — attach the agent via a real `-javaagent` at JVM
startup for the entire test run, instead of per-class dynamic self-attach — closes this
whole bug class permanently rather than patching around it one test class at a time.
Full writeup: `docs/DESIGN_DECISIONS.md` §4G.

The JNDI sensor remains the one open item after real effort: four attempts (bootstrap
visibility, a JPMS module-read grant, static vs. dynamic attach, and a bytecode-dump
diagnostic that itself didn't work as configured) all failed to explain or fix why
`InitialContext#lookup(String)`'s woven advice never executes despite ByteBuddy reporting
a clean retransform. Documented in full in `docs/DESIGN_DECISIONS.md` §6, left `@Disabled`
with a clear reason rather than deleted or silently left failing.

`OneClassDistanceDetector` is deliberately not wired into enforcement: its own test
(`demonstratesTheDocumentedLimitation_sameShapeDifferentCallerIsNotCaught`) proves it
can't distinguish a benign caller from a malicious one producing the same call shape —
exactly the gap the caller-identity-based `AllowlistDetector` fills today, and exactly
the gap a future structure-aware model (GNN) would need to close properly.
