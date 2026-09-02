# 01 — Basics

**Concept:** classes, objects, constructors, static/final, access modifiers, enums,
interfaces, abstract classes, encapsulation.

**Example:** `Basics.java` — a small `Shape` hierarchy (`Circle`, `Rectangle`) built on
an abstract base class implementing a common interface, plus an `Access` enum.

**Why it matters:** this is the foundation everything else builds on. Immutable,
encapsulated value types (private final fields, no setters) are what make it safe to
pass objects like `ProvenanceEvent` between threads without extra locking.

**Where ProvGuard uses it:** `provguard.provenance.SinkType` (enum),
`provguard.provenance.ProvenanceEvent`/`EventBuffer` (encapsulation + immutability).

Run: `java Basics.java`
