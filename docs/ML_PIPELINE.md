# ML Pipeline

**Status: a real Python-to-ONNX-to-Java pipeline exists and runs end to end; the currently
trained model is not good enough to enforce with, and that's documented, not hidden. The
GNN/deep-learning stretch goal does not exist.**

Two real, tested rungs exist today:

1. `provguard.detection.OneClassDistanceDetector` — a benign-only, statistical (not
   deep-learning) novelty detector, genuinely trained on `FeatureVector`s extracted from
   real `ProvenanceGraph`s produced by actually triggering the ProcessBuilder sensor (see
   `OneClassDistanceDetectorTest`, which captures real data, not invented numbers). It is a
   z-score-style distance check over two features (call-stack depth, edge count).
2. `ml/train_and_export.py` (scikit-learn `OneClassSVM`) → `ml/one_class_svm.onnx` →
   `provguard.detection.OnnxOneClassDetector` (real ONNX Runtime Java inference, see
   `OnnxOneClassDetectorTest`) — the actual Python-train/Java-infer split the research plan
   calls for. **Real result, honestly reported: 0% recall on the local dataset.** Root
   cause verified (not guessed): the local dataset has zero real feature variance per
   sink-type/label group — see `evaluation/results/onnx-svm-evaluation-2026-09-03.md` for
   the full diagnosis. The pipeline (train → export → load → infer) genuinely works; the
   model it currently produces does not generalize, because the data it was trained on has
   nothing to generalize from. Not wired into enforcement for that reason, same as (1).

**A real, documented limitation, made concrete by a test
(`demonstratesTheDocumentedLimitation_sameShapeDifferentCallerIsNotCaught`):**
depth/edge-count alone cannot distinguish a benign caller from a malicious one that
happens to produce a structurally identical call (same depth, same edge count). This is
exactly why `AllowlistDetector` (caller-identity-based) is the detector actually wired
into `provguard.enforcement.Policy` today — `OneClassDistanceDetector` is real and tested
but not yet plugged into enforcement, because it isn't yet good enough to replace the
allowlist.

## Planned approach (from the original ProvGuard research plan)

1. Train in Python, infer in Java — a deliberate split so the real-time system stays a
   pure-Java artifact at demo time. **Done** (`ml/train_and_export.py` +
   `OnnxOneClassDetector`, ONNX Runtime Java — the decision was made in favor of ONNX
   Runtime over DJL for simplicity: one dependency, no separate model-server abstraction).
2. Baselines to build first (in rough order of simplicity):
   - One-Class SVM over hand-crafted path features — **done, real model trained and
     exported, real result is 0% recall on this dataset (see above)**
   - Autoencoder over path embeddings — not attempted
   - GNN (GraphSAGE/GIN) over `ProvenanceGraph` structure — the stretch goal, not
     attempted; structure is what should let it generalize to unseen gadget chains,
     unlike the allowlist
3. Export trained models to ONNX; load and run inference from `provguard.detection` via
   ONNX Runtime Java — **done** (`com.microsoft.onnxruntime:onnxruntime` in `pom.xml`).

## What NOT to expect here

No claim that the trained model is fit for enforcement — it isn't, and the reason why is
fully diagnosed and written up rather than hidden (see
`evaluation/results/onnx-svm-evaluation-2026-09-03.md`). No autoencoder, no GNN, no
comparison against either. No ROC-AUC or threshold sweep for the ONNX model.
