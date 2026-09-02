# Evaluation

**Status: real detection metrics and a real overhead benchmark exist. Held-out
zero-day-simulated evaluation against a real exploit dataset does not.**

## What's actually measured (real numbers, not estimates)

- **Detection metrics** (precision/recall/F1/FPR) for `AllowlistDetector` and
  `OneClassDistanceDetector` (pooled and per-sink-type) against the real local dataset —
  see `evaluation/results/detection-evaluation-2026-09-03.md` for full numbers and
  honest interpretation. Reproduce: `java -cp target/classes provguard.tools.Evaluator`.
- **Runtime overhead** for the deserialization sensor, ALLOW path — see
  `evaluation/results/deserialization-overhead-2026-09-03.md`. Real result: ~19-21x
  baseline overhead, reduced to ~14-15x after one targeted, measured optimization
  (`StackWalkerCollector.MAX_FRAMES` 20→15). Reproduce:
  `evaluation/benchmarks/DeserializationOverheadBenchmark.java`.

## What the original plan calls for that is still NOT measured

- Detection of held-out **zero-day-simulated** chains from a real exploit-chain dataset
  (no such dataset exists — see `docs/DATASET.md`; the local dataset's "malicious" label
  means "wrong caller," not "real gadget chain")
- Comparison against a signature/RASP baseline other than `AllowlistDetector` itself
  (there is no separate rule-based baseline implemented)
- Comparison against any learned model (none exists — see `docs/ML_PIPELINE.md`)
- ROC-AUC (no threshold sweep was performed, even though `OneClassDistanceDetector` has a
  continuous distance score that could support one)
- Latency/throughput/memory for `ProcessBuilder`/`InitialContext` sensors specifically
  (only `DESERIALIZATION` was benchmarked, deliberately — see that benchmark's own
  Javadoc for why `ProcessBuilder` is a poor target and `InitialContext` doesn't work at
  all yet)
- Any real CVE reproduction (Log4Shell, a Commons-Collections deserialization CVE, a SpEL
  injection CVE) — none attempted

## Structure

```
evaluation/
    benchmarks/   DeserializationOverheadBenchmark.java (real, run)
    results/      deserialization-overhead-2026-09-03.md, detection-evaluation-2026-09-03.md (real, dated)
    scripts/      (empty - no separate scripts beyond the benchmark/evaluator themselves)
    reports/      (empty - the results/ files above ARE the reports)
```
