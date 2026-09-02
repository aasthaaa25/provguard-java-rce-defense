# Evaluation

**Status: NOT YET MEASURED.** No benchmark has been run. Every metric below is planned,
not reported — per project policy, nothing here is a real number until it's produced by
an actual measurement.

## What the original plan calls for measuring

- Detection: precision, recall, F1, ROC-AUC, and — most importantly — detection of
  held-out (simulated zero-day) gadget chains never seen during training/allowlist
  construction
- Runtime overhead: latency/throughput with the agent off vs. monitoring vs. (eventually)
  ML inference vs. blocking, plus memory footprint
- Comparison against baselines: this repo's own `AllowlistDetector` already **is** the
  signature/allowlist baseline the plan calls comparing against — a future learned model
  needs to beat it, specifically by generalizing to callers the allowlist was never told
  about

## What can honestly be said today

- The enforcement mechanism (`ALLOW`/`LOG`/`BLOCK`) has been functionally verified: tests
  and a live demo confirm that `BLOCK` mode genuinely prevents the underlying
  `ProcessBuilder`/`ObjectInputStream` call from executing, for a caller not on the
  allowlist.
- No latency/throughput/memory numbers have been measured. No claim of "low overhead" is
  made anywhere in this repository's other docs for exactly this reason.
- No comparison against any baseline other than "does `AllowlistDetector` itself work" has
  been performed, because no second detector (learned or otherwise) exists yet to compare
  against.

## Structure (once evaluation work actually starts)

```
evaluation/
    benchmarks/   # JMH or similar overhead benchmarks - none exist yet
    results/      # raw output from real runs - none exist yet
    scripts/      # reproducible evaluation scripts - none exist yet
    reports/      # written summaries of the above - none exist yet
```
