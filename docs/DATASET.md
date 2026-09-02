# Dataset

**Status: a real, small, local dataset exists — it is NOT the real exploit-chain dataset
the original plan calls for.** See `dataset/README.md` for the full honesty note; summary
here.

## What actually exists

`provguard.tools.DatasetGenerator` (source in `src/main/java/provguard/tools/`) actually
triggers real `ProcessBuilder`/`ObjectInputStream` sink calls through the live agent —
both from a trusted caller (benign) and from a caller deliberately absent from the
trusted allowlist (malicious-*shaped*) — and writes the genuinely captured
depth/edge-count/caller data to:

```
dataset/
    benign/local-benign-traces.jsonl              (40 real captured traces)
    malicious/local-malicious-shaped-traces.jsonl  (40 real captured traces)
```

Every value in those files came from an actual `StackWalker` capture during a real run —
not invented, not hand-typed.

## What does NOT exist (stated plainly, per project policy)

- No real exploit-chain integration: no `ysoserial`, `GCMiner`, or
  `JNDI-Injection-Exploit-Plus` payloads are used or bundled. The "malicious" label means
  "structurally similar call from an untrusted caller," not "a real gadget chain."
- No `fixtures/vulnerable-*` targets were attacked with real payloads to generate this
  data — the two existing fixtures (`fixtures/vulnerable-command-execution`,
  `fixtures/vulnerable-deserialization`) demonstrate real blocking but were not used as
  the basis for this dataset (the `DatasetGenerator` calls the sinks directly).
- `dataset/schemas/` and `dataset/processed/` (mentioned in the original plan's directory
  layout) don't exist as separate artifacts — the schema is documented inline in
  `dataset/README.md`'s table instead, and there's no separate feature-processing step
  beyond what `DatasetGenerator` already does at capture time.

## Why this is still useful, honestly

It's real enough to exercise both existing detectors meaningfully:
`AllowlistDetector` (caller-identity-based) and `OneClassDistanceDetector` (depth/edge-count
structural features) both operate on exactly this kind of data, and `docs/EVALUATION.md`
reports real precision/recall/F1 numbers computed against it. What it can't honestly
support is any claim about detecting *real* gadget chains, since none exist in this
dataset — that remains real follow-up work.
