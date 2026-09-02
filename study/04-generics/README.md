# 04 — Generics

**Concept:** generic classes, generic methods, bounded type parameters (`T extends
Comparable<T>`), wildcards (`? extends Number`).

**Example:** `Generics.java` — a `Ranked<T>` container, a generic `firstOrDefault`
method, and a wildcard-accepting `sumOf`.

**Why it matters:** ProvGuard's planned `ProvenanceGraph<N extends GraphNode>` needs to
be reusable/testable independent of any specific sensor, exactly like `Ranked<T>` here
is reusable independent of what `Comparable` type it holds.

Run: `java Generics.java`
