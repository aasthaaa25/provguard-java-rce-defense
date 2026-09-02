# ONNX One-Class SVM Evaluation — Real Measurement, Real Negative Result

**Date:** 2026-09-03
**Tool:** `ml/train_and_export.py` (scikit-learn `OneClassSVM`, exported via `skl2onnx`)
**Dataset:** same `dataset/benign` + `dataset/malicious` local dataset used for
`evaluation/results/detection-evaluation-2026-09-03.md`.

This documents a genuine attempt to close the gap noted in `docs/ML_PIPELINE.md` (no real
trained model existed). The result is **not** a success — it's an honestly-diagnosed
failure, kept because the failure mode is itself a real, useful finding about this
project's current dataset, not a hidden or glossed-over gap.

## What was tried

1. Pooled training (both sink types together) with sink type as a one-hot feature
   (`[depth, edgeCount, is_process_execution, is_deserialization]`), `nu=0.05`:
   `TP=0 FP=0 TN=10 FN=40` — caught nothing.
2. Same, `nu=0.3` (tighter expected-outlier-fraction bound): still `TP=0 FP=0 TN=10 FN=40`.
3. Per-sink-type training (mirroring the fix that worked for
   `OneClassDistanceDetector` — see `detection-evaluation-2026-09-03.md`), `nu=0.3`:
   **worse** — flagged *every* test point, including held-out benign points identical to
   the training data, as anomalous.

## Root cause (verified by direct inspection, not guessed)

Printing the raw feature vectors shows the local dataset has **zero within-group
variance**: every one of the 20 captured benign `PROCESS_EXECUTION` traces is bit-identical
(`depth=3, edgeCount=2`); every malicious-shaped one is bit-identical (`depth=4,
edgeCount=3`). Same story for `DESERIALIZATION` (`depth=10, edgeCount=9` for both labels —
already documented as indistinguishable). In other words, the entire 80-trace dataset
reduces to **4 distinct feature-vector points**, each repeated up to 20 times, because
`DatasetGenerator` calls the exact same synthetic code path in a loop rather than sampling
real-world call-site diversity.

`sklearn.svm.OneClassSVM` has no safeguard for this (unlike
`provguard.detection.OneClassDistanceDetector`, which explicitly floors its std-dev
estimate at 0.5 to avoid exactly this problem — see its Javadoc). Fit on duplicate-heavy,
near-zero-variance, bimodal (two widely separated clusters, `PROCESS_EXECUTION` around
depth 3-4 and `DESERIALIZATION` around depth 9-10) data:

- **Pooled**: `decision_function` on every training and held-out benign point was exactly
  `0.0` (sitting exactly on the margin — a sign of a degenerate fit), while malicious
  `PROCESS_EXECUTION` points scored *positive* (i.e. more inlier than the training data
  itself) at every `nu` tried. With so few unique points and a global RBF bandwidth that
  has to span both clusters, the boundary geometry does not behave the way it would on a
  real, continuously-varying feature distribution.
- **Per-sink-type**: with training reduced to a single repeated point, the fit is even
  more degenerate — it flagged its own held-out identical test points as outliers, which
  is a clear sign the model has nothing meaningful to fit, not a real detection signal.

## Honest conclusion

This is **not** evidence that One-Class SVM or ONNX export don't work — the pipeline
itself (`ml/train_and_export.py` → `ml/one_class_svm.onnx` → Java inference, see
`provguard.detection.OnnxOneClassDetector`) runs end-to-end and produces a real, loadable
ONNX model. The failure is specifically that **this project's current dataset has no real
variance to learn from** — it was built to demonstrate the pipeline plumbing and the
already-known allowlist-vs-structural gap (see `dataset/README.md`), not to provide enough
natural diversity for a distance/kernel-based model to generalize.

**What would fix this** (not attempted — out of scope for the current pass): capture
traces from genuinely varied call sites/depths (different real call chains hitting the
same sink), not N repetitions of one synthetic method. Recorded here as the concrete
next step rather than left implicit.

## What is still NOT measured

- Any real precision/recall for a learned model on data with genuine variance
- A GNN or autoencoder baseline (never attempted — see `docs/ML_PIPELINE.md`)
- Whether ONNX Runtime Java inference numerically matches scikit-learn's own predictions
  bit-for-bit on the same input (checked only for basic load/run success, not agreement —
  see `OnnxOneClassDetectorTest`)
