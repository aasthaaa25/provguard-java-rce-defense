# 05 — Collections

**Concept:** `List`/`Set`/`Map`/`Deque`/`PriorityQueue`, a correct `equals()`/`hashCode()`
contract, `Comparator`, `Collections` utilities.

**Example:** `CollectionsDemo.java` — a `CallFrame` value type used as both a `Set`
element and a `Map` key, a `Deque` used as a call stack, and a `PriorityQueue` with a
custom `Comparator`.

**Why it matters:** ProvGuard's planned `GraphNode`/`GraphEdge` need exactly this kind
of correct `equals`/`hashCode` to deduplicate repeated call paths in a `Set`.

**Where ProvGuard uses it today:** `provguard.provenance.EventBuffer` uses
`CopyOnWriteArrayList` + `List.copyOf(...)` for its read-only view, same idea as the
`Collections.unmodifiableList` shown here.

Run: `java CollectionsDemo.java`
