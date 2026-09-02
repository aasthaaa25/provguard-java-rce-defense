# 03 — Records, Sealed Interfaces, Pattern Matching

**Concept:** records (immutable data carriers), sealed interfaces (closed type
hierarchies), pattern-matching `switch`, `var`, text blocks.

**Example:** `RecordsSealed.java` — a sealed `Decision` (`Allow`/`Log`/`Block`) switched
over exhaustively with no `default` branch needed, because the compiler knows there are
no other implementations possible.

**Why it matters:** this is exactly the shape ProvGuard's planned enforcement layer
needs — a `Decision` type where adding a new kind of decision forces every switch over
it to be updated, at compile time, everywhere.

**Where ProvGuard uses it today:** `provguard.provenance.ProvenanceEvent` is already a
record.

Run: `java RecordsSealed.java`
