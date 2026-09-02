# Detection Evaluation — Real Measurement

**Date:** 2026-09-03
**Tool:** `provguard.tools.Evaluator` (source: `src/main/java/provguard/tools/Evaluator.java`)
**Dataset:** `dataset/benign/local-benign-traces.jsonl` + `dataset/malicious/local-malicious-shaped-traces.jsonl`
(40 + 40 real captured traces — see `dataset/README.md`'s honesty note before
interpreting "malicious" here)

Run: `java -cp target/classes provguard.tools.Evaluator`

## Results

### AllowlistDetector (the detector actually wired into `provguard.enforcement.Policy`)

```
TP=40 FP=0 TN=40 FN=0
Precision: 1.000   Recall: 1.000   F1: 1.000   FPR: 0.000
```

**Honest caveat, not hidden:** this is a perfect score *by construction*, not a
surprising result. The dataset's "malicious" label is defined as "caller absent from the
allowlist," and `AllowlistDetector` detects exactly that condition — evaluating it
against this specific dataset is close to tautological. It's included for completeness
and as a sanity check (a bug in either the detector or the dataset generator would show
up as something other than a perfect score here), not as evidence the allowlist approach
"solves" detection in general — see `docs/THREAT_MODEL.md` for what it genuinely does and
doesn't cover.

### OneClassDistanceDetector, pooled across sink types (deliberately naive)

```
TP=0 FP=0 TN=10 FN=40
Precision: 0.000   Recall: 0.000   F1: 0.000   FPR: 0.000
```

Trained on all 30 training-split benign traces (both sink types mixed together) and
caught **nothing**. This is the exact failure mode documented in
`docs/DESIGN_DECISIONS.md` and `OneClassDistanceDetectorTest`: `DESERIALIZATION`'s
natural depth (~10, dominated by `ObjectInputStream`'s own internal call chain) is so
different from `PROCESS_EXECUTION`'s (~3-4) that the pooled training set has huge
variance, producing a tolerance band wide enough to swallow every real deviation within
either sink type.

### OneClassDistanceDetector, trained per sink type (correct methodology)

```
  PROCESS_EXECUTION: TP=20 FP=0 TN=5  FN=0
  DESERIALIZATION:   TP=0  FP=0 TN=5  FN=20
TP=20 FP=0 TN=10 FN=20
Precision: 1.000   Recall: 0.500   F1: 0.667   FPR: 0.000
```

A genuinely informative split:
- **`PROCESS_EXECUTION`: caught 100% (20/20)** of the malicious-shaped traces. In the
  captured data, a call routed through the untrusted `UntrustedSampleSource` wrapper class
  has depth 4 vs. the trusted direct call's depth 3 — a real, structurally detectable
  difference, and per-sink-type training (zero variance among the homogeneous benign
  training samples, floored std-dev) was sensitive enough to catch it.
- **`DESERIALIZATION`: caught 0% (0/20)**. Checked directly against the raw dataset:
  benign and malicious-shaped `DESERIALIZATION` traces both show **depth=10, edgeCount=9
  — identical**. The extra stack frame from routing through `UntrustedSampleSource` does
  not show up as a depth difference for this sink the way it did for `PROCESS_EXECUTION`
  in this captured data. Structural features genuinely have nothing to catch here; this
  is real data, not a detector bug.

Overall (both sink types combined): **precision 1.0, recall 0.5, F1 0.667** — no false
positives, but only half the malicious-shaped traces caught, entirely explained by the
`DESERIALIZATION` gap above.

## Honest interpretation

- `AllowlistDetector`'s perfect score is expected and not very informative on its own
  (see caveat above) — its real value is that it's what's actually enforced today, and it
  fails closed on unknown callers, which this dataset doesn't stress-test.
- `OneClassDistanceDetector` is a genuinely useful, if limited, structural signal for
  `PROCESS_EXECUTION` in this data, and genuinely useless for `DESERIALIZATION` in this
  data — both real findings, not assumptions. This is exactly the gap a richer feature
  set (or a real GNN over the full graph structure, not just depth/edge-count) would need
  to close, per `docs/ML_PIPELINE.md`.
- **None of this evaluates detection of a real gadget chain** — see `dataset/README.md`'s
  honesty note. These numbers describe how well each detector distinguishes "caller
  on/off an explicit list," not "benign vs. actual malicious exploit," which is a
  materially different and harder problem this project has not attempted to evaluate.

## What is still NOT measured

- Real gadget-chain detection (no `ysoserial`/`GCMiner`/similar integration exists)
- ROC-AUC (only one detector output type — anomalous/not — is available; no continuous
  score to sweep a threshold over for `AllowlistDetector`; `OneClassDistanceDetector`
  does have a continuous distance score, but a proper ROC curve over it was not computed
  here)
- Any comparison against a real learned model (none exists yet — see `docs/ML_PIPELINE.md`)
