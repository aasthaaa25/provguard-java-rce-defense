# 10 — CompletableFuture

**Concept:** async pipeline composition (`thenApply`, `thenCombine`, `exceptionally`).

**Example:** `CompletableFutureDemo.java` — a mini `capture -> featurize -> score`
pipeline chained through `CompletableFuture`, plus combining two independent futures.

**Why it matters:** this is the exact shape of ProvGuard's planned detection pipeline
(capture → featurize → score → decide) — chained so the thread that triggered the sink
is never blocked waiting for detection to finish.

Run: `java CompletableFutureDemo.java`
