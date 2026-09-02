# ML Pipeline

**Status: not started.** No training code, no model, no ONNX export, no Java-side
inference exists in this repository. This document records the plan, not a claim of
progress — see `README.md` "Current status" and `docs/WEEKLY_ROADMAP.md` for what
actually exists (`provguard.detection.AllowlistDetector`, a non-learned baseline).

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
