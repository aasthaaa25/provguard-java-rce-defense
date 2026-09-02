# ML Pipeline

**Status: a real first rung exists; the GNN/deep-learning stretch goal does not.** No
Python training code, no ONNX export, no DJL/ONNX-Runtime-Java inference exists in this
repository. What DOES exist, real and tested: `provguard.detection.OneClassDistanceDetector`
— a benign-only, statistical (not deep-learning) novelty detector, genuinely trained on
`FeatureVector`s extracted from real `ProvenanceGraph`s produced by actually triggering
the ProcessBuilder sensor (see `OneClassDistanceDetectorTest`, which captures real data,
not invented numbers). It is a z-score-style distance check over two features (call-stack
depth, edge count) — the honest first step of the "learned model" ladder in the original
research plan, not a placeholder and not a GNN.

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
   pure-Java artifact at demo time.
2. Baselines to build first (in rough order of simplicity):
   - One-Class SVM over hand-crafted path features
   - Autoencoder over path embeddings
   - GNN (GraphSAGE/GIN) over `ProvenanceGraph` structure — the stretch goal; structure
     is what should let it generalize to unseen gadget chains, unlike the allowlist
   3. Export trained models to ONNX; load and run inference from
   `provguard.detection` via ONNX Runtime Java (decision between that and DJL not yet
   made — see `docs/DESIGN_DECISIONS.md`).

## Prerequisite work not yet done

- A real dataset (see `docs/DATASET.md`) — no ML training can start without one
- Feature extraction beyond what `AllowlistDetector` needs (today's `ProvenanceGraph` is
  intentionally minimal — caller identity, depth, edges — not yet a feature vector
  suitable for a model)
- A decision on ONNX Runtime Java vs. DJL, deferred until this module is actually built

## What NOT to expect here

No benchmark numbers, no trained model file, no accuracy/precision/recall claims — any of
those would be fabricated if stated without a real training run against real data,
neither of which exist yet.
