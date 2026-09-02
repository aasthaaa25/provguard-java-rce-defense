# Deserialization Overhead — Real Measurement

**Date:** 2026-09-03
**Machine:** dev machine used throughout this project (JDK 26.0.2, see `docs/DESIGN_DECISIONS.md` §2)
**Benchmark:** `evaluation/benchmarks/DeserializationOverheadBenchmark.java`
**Methodology:** 5,000 warmup iterations, 50,000 measured iterations, `System.nanoTime()`
around a real `ObjectInputStream#readObject()` call deserializing a small object. Not a
JMH benchmark (no per-trial JVM fork, no dead-code-elimination blackhole beyond the
warmup) — real measurements, but treat absolute numbers as indicative, not
publication-grade.

## Results (2 trials each)

| Scenario | Trial 1 | Trial 2 |
|---|---|---|
| No agent (baseline) | 4.784 µs/call | 4.392 µs/call |
| Agent attached, ALLOW path (monitoring only) | 98.374 µs/call | 90.668 µs/call |

**Overhead: ~19–21x** relative to the unmonitored baseline (roughly +85–94 microseconds
added per deserialization call).

## Honest interpretation

This is **not** the "single-digit percent overhead" the original research plan aspired
to (comparable to how DeseriGuard/JFR are reported to perform) — it's a real,
significantly higher multiplier. In absolute terms it's still small (under 0.1ms per
call), which matters little for an occasional deserialization call but would matter for
a hot path doing many per second.

**Where the cost almost certainly comes from** (not yet profiled/broken down — this is
informed reasoning, not a measured breakdown, and is flagged as such):
- `StackWalker.walk(...)` capturing up to 20 frames per call (`StackWalkerCollector`)
- Building a `ProvenanceGraph` (allocating `GraphNode`/`GraphEdge` objects, a `HashSet` for
  edges) on every single call, including the ALLOW path
- `AllowlistDetector`'s `Set.contains()` check is cheap by comparison and is almost
  certainly not the dominant cost

**What was NOT measured, honestly:**
- The `TraceWriter` async path was disabled for this benchmark (`Policy.tracingEnabled`
  defaults to `false`) — its overhead (submitting to a virtual-thread executor) is a
  separate, unmeasured cost
- No profiler was attached to actually attribute the ~85-94µs to specific method calls —
  the "almost certainly" explanation above is a hypothesis, not a proven breakdown
- `ProcessBuilder`/JNDI overhead was not benchmarked (see
  `evaluation/benchmarks/DeserializationOverheadBenchmark.java`'s own Javadoc for why
  `ProcessBuilder` specifically is a poor benchmark target — OS process-spawn cost would
  swamp the signal)

## Follow-up: one optimization actually tried and measured

Acted on the first hypothesis above: reduced `StackWalkerCollector.MAX_FRAMES` from 20 to
15 (the smallest value that still passed `DeserializationSensorTest` — 8 was tried first
and was too aggressive, causing `ObjectInputStream`'s own internal call chain to fill the
entire capture with no differently-classed caller frame left, which made
`AllowlistDetector` fail closed and block a legitimately-allowed call. 15 leaves enough
headroom).

| Scenario | Before (MAX_FRAMES=20) | After (MAX_FRAMES=15) |
|---|---|---|
| Agent attached, ALLOW path | ~90–98 µs/call | **67.345 µs/call** |

A real, measured **~28–32% reduction** in per-call overhead from this one change alone.
Total overhead relative to baseline is now roughly **14–15x** rather than ~19–21x — still
significant, and still not "single-digit percent," but a genuine, verified improvement
from the very first hypothesis tested, not a guess left unverified.

## Follow-up work this motivates

- Profile the ALLOW path (e.g. `async-profiler` or JFR CPU sampling) to actually attribute
  the overhead rather than guess
- Consider capturing fewer `StackWalker` frames (`MAX_FRAMES` in `StackWalkerCollector` is
  currently 20 — likely far more than `AllowlistDetector` or `OneClassDistanceDetector`
  actually need)
- Consider caching/reusing graph-building allocations, or skipping full graph
  construction for the common ALLOW case
