# Dataset

**Status: not started.** No dataset — benign or malicious — has been collected or
generated. This document records the plan only.

## Planned structure (from the original ProvGuard research plan)

```
dataset/
    benign/       # traces from real, legitimately-running code
    malicious/    # traces from reproduced exploit chains (ysoserial, JNDI-Injection-Exploit-Plus, etc.)
    processed/    # feature-extracted, ready for training (not committed if large)
    schemas/      # documented structure of the above
```

## Planned sources

- **Benign traces**: running this repository's own `DemoMain` and sensor tests already
  produces real `ProvenanceEvent`/`ProvenanceGraph` instances for the two working sinks —
  that data is not yet being persisted/collected into a dataset, but the capture
  mechanism to do so already exists (`provguard.provenance.EventBuffer`,
  `provguard.graph.GraphBuilder`).
- **Malicious traces**: the plan calls for reproducing real gadget chains via `ysoserial`
  and JNDI injection payloads against local, self-contained vulnerable fixtures — no such
  fixtures exist in this repository yet (the `fixtures/` directory from the original plan
  was never created).

## Why nothing is committed here yet

Per the original plan's own guidance: commit small examples/schemas, not large generated
datasets, and provide reproducible generation scripts rather than static data dumps.
Since no generation pipeline exists yet, there is nothing to commit that wouldn't be
fabricated placeholder data.
